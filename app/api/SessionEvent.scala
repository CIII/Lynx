package api

import java.io.InputStream
import java.util.Properties
import javax.inject.Inject

import com.google.inject.Provider
import com.redis.RedisClientPool
import dao._
import models.{Event, EventAttribute, SessionAttribute, Url}
import org.joda.time.{DateTime, DateTimeZone}
import org.uaparser.scala.Parser
import play.api.{Application, Environment, Logger, Play}
import play.api.mvc._
import play.mvc.Http
import utils.utilities._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.matching.Regex
import scala.annotation.meta.getter
import scala.annotation.meta.field

/**
  * Created by slin on 2/6/17.
  */


object SessionEvent{

  var GIT_HASH: String = _
  var ROBOTS: List[Robot] = _

  object PageType extends Enumeration{
    val Index = Value("index")
    val Index_NEW = Value("index_new")
    val Editorial = Value("editorial")
    val GetQuotes = Value("get_quotes")
    val Compare160801 = Value("compare_160801")
    val Compare160802 = Value("compare_160802")
    val ThankYou = Value("thank_you")
    val Installers = Value("installers")
    val PrivacyPolicy = Value("privacy_policy")
    val TermOfUse = Value("term_of_use")
    val MiddleClassSolarIncentive = Value("middle_class_solar_incentive")
    val ThirdParty = Value("third_party")
    val Financing = Value("financing")
    val SolarRebates = Value("solar_rebates")
    val SolarCityDeals = Value("solar_city_deals")
    val City = Value("city")
    val ElectricitySavings = Value("electricity_savings")
    val EnergySavings = Value("energy_savings")
    val Preview = Value("preview")
  }

  case class Robot(pattern: String)
  object Robot {
    implicit val robotReads: Reads[Robot] = (
      (JsPath \ "pattern").read[String]
      ).map(Robot.apply _)
  }

  def populateRobots = {
    val robotJson = scala.io.Source.fromFile("conf/bot/crawler-user-agents.json").mkString
    Json.parse(robotJson).validate[List[Robot]] match {
      case s: JsSuccess[List[Robot]] => {
        val r: List[Robot] = s.get
        ROBOTS = r
      }
      case e: JsError => {
        Logger.info("Unable to load list of robots")
        Logger.info(e.errors.toString)
        ROBOTS = List[Robot]()
      }
    }
  }

  var ATTRIBUTES = Map[String, Long]()
  var DOMAINS = Map[String, Long]()
  def loadValues(attributeDAO: AttributeDAO, domainDAO: DomainDAO) = {
    ATTRIBUTES.size match {
      case 0 =>
        attributeDAO.all().map {
          db_attributes =>
            db_attributes.map {
              attribute =>
                ATTRIBUTES += (attribute.name.toLowerCase -> attribute.id.get)
            }
        }
      case _ =>
    }
    DOMAINS.size match {
      case 0 =>
        domainDAO.all().map {
          domains =>
            domains.map {
              domain => DOMAINS += (domain.domain.toLowerCase -> domain.id.get)
            }
        }
      case _ =>
    }
  }

  try {
    populateRobots
  }catch{
    case e: Throwable =>
      e.printStackTrace()
  }
}

/** A trait that is applied to [[Controller]]s, for reasons I haven't understood yet. */
trait SessionEvent {
  self: Controller =>

    @(Inject @getter) val sessionDAO: SessionDAO
    @(Inject @getter) val domainDAO: DomainDAO
    @(Inject @getter) val eventTypeDAO: EventTypeDAO
    @(Inject @getter) val eventDAO: EventDAO
    @(Inject @getter) val attributeDAO: AttributeDAO
    @(Inject @getter) val eventAttributeDAO: EventAttributeDAO
    @(Inject @getter) val urlDAO: UrlDAO
    @(Inject @getter) val sessionAttributeDAO: SessionAttributeDAO
    @(Inject @getter) implicit val configuration: play.api.Configuration

    import SessionEvent._

    loadValues(attributeDAO, domainDAO)

  def persist_page_type(pageType: String, eventId: Long) = {
    val PAGE_TYPE = "page_type"
    attributeDAO.findByAttribute(PAGE_TYPE).map {
      case Some(attribute) =>
        eventAttributeDAO.insert(
          EventAttribute(
            id = Some(0L),
            event_id = Some(eventId),
            attribute_id = attribute.id,
            value = Some(pageType),
            created_at = Some(new DateTime(DateTimeZone.UTC)),
            updated_at = Some(new DateTime(DateTimeZone.UTC))
          )
        )
      case None =>
        Logger.error("[PAGE SERVING] Could not persist page_type")
    }
  }

    def create_persist_session_and_event(force_create_page_serve_event: Boolean, separatePageRender: Boolean)
                                        (implicit request: Request[AnyContent], environment: Environment): (Session, Event) = {
      val page_served_id = request.session.get(PAGE_SERVED_ID)

      /**
       * Check if a new session needs to be created, and if so create it.
       */
      def check_create_session: Future[models.Session] = {
        request.session.get(SESSION_ID) match {
          case Some(sid) =>
            // If there is a session Id, check if the session has timed out. If it has, then we need to create a new session.  
            request.session.get(SESSION_TIMEOUT_ID) match {
              case Some(timeout) =>
                val timeout_ts = DateTime.parse(timeout)
                if(new DateTime(DateTimeZone.UTC).isAfter(timeout_ts)){
                  Logger.debug("Session timed out - creating new session")
                  return create_session
                }
              case None =>
                throw new IllegalStateException(s"If a session exists, a session timeout must exist, and this condition failed in session ${sid}.");
            }

            // If the query string contains utm_source, then we need to check if we're coming from a new ad click vs. a refresh
            // or back to the page which already contained it.  For this, we check against cookies, if the campaign, adgroup, and ad
            // all match then it is a back/refresh/same ad click.  If they differ, then the user clicked a new ad and we should create
            // a new session.  For scenarios where the request is empty but the cookie contains the utm parameters we will not consider
            // this a differ.
            if(!request.queryString.get(UTM_SOURCE).isEmpty){
                Logger.debug("Query contains utm_source - Checking cookies to match session to existing")
                val matchCriteria = List(UTM_CAMPAIGN, ADGROUPID, UTM_CONTENT)
                matchCriteria.foreach { 
                  criteriaId => 
                    val cookieCriteria = request.session.get(criteriaId);
                    val requestCriteria = request.getQueryString(criteriaId);
                    if(!cookieCriteria.isEmpty && !requestCriteria.isEmpty){
                      if(!cookieCriteria.get.equals("") && !requestCriteria.get.equals("") && 
                          !cookieCriteria.get.equals(requestCriteria.get)){
                        Logger.debug(s"mismatch for $criteriaId - request: $requestCriteria cookie: $cookieCriteria")
                        return create_session
                      }  
                    }
                }
                
                Logger.debug("UTM parameters match or had missing criteria, source is the same")
            }

            // If the above conditions pass, attempt to get the session from the db by id.  If we retrieve a valid session return it,
            // else create a new one.
            sessionDAO.findBySessionId(sid.toLong) map {
              case Some(session) =>
                session
              case None =>
                Await.result(create_session, Duration.Inf)
            }
          
          case None =>
            Logger.debug("No Session Id - creating new session")
            //create event? need session id
            create_session
        }
      }

    def is_robot(userAgent: Option[String]): Boolean = {
      userAgent match {
        case Some(userAgent) =>
          for (robot <- ROBOTS)
            if(userAgent.toLowerCase.contains(robot.pattern.toLowerCase))
              return true
          false
        case _ =>
          false
      }
    }

    def create_session: Future[models.Session] = {
      Logger.debug("Creating new session")
      val domain = utils.utilities.get_domain_name()
      Logger.debug(s"Domain gotten: ${domain}")
      sessionDAO.insert(
        models.Session(
          id = Some(0L),
          domain_id = DOMAINS.get(domain),
          browser_id = None,
          ip = Some(utils.utilities.getClientIp(request)),
          user_agent = request.headers.get(Http.HeaderNames.USER_AGENT),
          traffic_source_id = None,
          is_robot = is_robot(request.headers.get(Http.HeaderNames.USER_AGENT)),
          created_at = Some(new DateTime(DateTimeZone.UTC)),
          updated_at = Some(new DateTime(DateTimeZone.UTC)),
          last_activity = Some(new DateTime(DateTimeZone.UTC))
        )
      )
    }


    //TODO NEED TO ADD PAGE TYPE
    def create_page_rendered_event(sid: Long): Future[Option[Event]] = {
      eventTypeDAO.findByEventName("Page Rendered").map {
        case Some(eventType) =>
          val page_rendered_url: String = {
            request.headers.get(Http.HeaderNames.REFERER) match {
              case Some(referer: String) if separatePageRender => referer
              case _ => request.uri
            }
          }
          Await.result({
            for {
              url <- urlDAO.insert(
                Url(
                  id = Some(0L),
                  url = page_rendered_url,
                  created_at = Some(new DateTime(DateTimeZone.UTC)),
                  updated_at = Some(new DateTime(DateTimeZone.UTC))
                )
              )
                event <-
                {
                  (page_served_id, force_create_page_serve_event) match {
                    case (None, _) | (_, true) =>
                      eventDAO.insert(
                        Event(
                          id = Some(0L),
                          event_type_id = eventType.id,
                          parent_event_id = None,
                          session_id = Some(sid),
                          url_id = url.id,
                          created_at = Some(new DateTime(DateTimeZone.UTC)),
                          updated_at = Some(new DateTime(DateTimeZone.UTC))
                        )
                      )
                    case _ => eventDAO.find(page_served_id.get.toLong).map { event => event.get }
                  }
                }
              } yield Some(event)
            }, Duration.Inf)
          case _ =>
            Logger.debug("Unable to create page rendered event, could not find event type")
            None
        }
      }

    val s_cookies =
      Await.result(
        for {
          session <- check_create_session
          parent_event <- create_page_rendered_event(session.id.get)
        } yield (session, parent_event), Duration.Inf
      )

    val sid = s_cookies._1.id.get
    val ps_id = s_cookies._2.get.id.get

    var new_session = request.session
    new_session = new_session + (SESSION_ID -> sid.toString)
    new_session = new_session + (SESSION_TIMEOUT_ID -> getTimeoutTs.toString)
    new_session = if (s_cookies._2.isEmpty) new_session else new_session + (PAGE_SERVED_ID -> ps_id.toString)
    
    // Append campaign, adgroupid, and content (which is ad), to the cookies so that we can
    // match these on the next request and check if a new session needs to be created. (see check_create_session)
    new_session = new_session + (UTM_CAMPAIGN -> (request.getQueryString(UTM_CAMPAIGN) match {
      case Some(utm_campaign) => utm_campaign
      case _ => ""
    }))
    new_session = new_session + (ADGROUPID -> (request.getQueryString(ADGROUPID) match {
      case Some(adgroupid) => adgroupid
      case _ => ""
    }))
    new_session = new_session + (UTM_CONTENT -> (request.getQueryString(UTM_CONTENT) match {
      case Some(utm_content) => utm_content
      case _ => ""
    }))

    persist_event_version(s_cookies._2.get)
    persist_session_attributes(s_cookies._1, request)

    (new_session, s_cookies._2.get)

  }

  private def persist_event_version(event: Event)(implicit environment: Environment): Unit ={

    try {
      val is = environment.classLoader.getResourceAsStream("git.properties")
      val property: Properties = new Properties()
      property.load(is)

      Option(GIT_HASH) match {
        case Some(hash) =>
        // Initialize git hash
        case _ =>
          GIT_HASH = property.getProperty("git_hash")
      }

      attributeDAO.findByAttribute("git_hash").map { attribute =>
        eventAttributeDAO.insert(
          EventAttribute(
            id = Some(0L),
            event_id = event.id,
            attribute_id = attribute.get.id,
            value = Some(SessionEvent.GIT_HASH),
            created_at = Some(new DateTime(DateTimeZone.UTC)),
            updated_at = Some(new DateTime(DateTimeZone.UTC))
          )
        )
      }
    }catch {
      case e: Exception =>
        Logger.error("[SESSION EVENT CREATION] Cannot load git hash, will not persist")
        Logger.error(e.getMessage)
    }
  }

  private def persist_session_attributes(session: models.Session, request: Request[AnyContent]): Unit = {

    var session_attributes = request.queryString;
    request.headers.get("user-agent") match {
      case Some(user_agent) => session_attributes += "os" -> Seq(Parser.get.parse(user_agent).os.family)
      case _ =>
    }
    request.headers.get("referer") match {
      case Some(referer) => session_attributes += "referer" -> Seq(referer)
      case _ =>
    }

    session_attributes += "request_url" -> Seq(request.host + request.uri)
    session_attributes.foreach {
      //Case where attribute is found
      case (attribute, value) =>
        ATTRIBUTES.get(attribute.toLowerCase) match {
          case Some(attribute_id) =>
            //add session attribute if doesn't already exist
            sessionAttributeDAO.findBySessionIdAndAttribute(session.id.get, attribute_id).map {
              case None =>
                sessionAttributeDAO.insert(
                  SessionAttribute(
                    id = Some(0L),
                    session_id = session.id,
                    attribute_id = Some(attribute_id),
                    value = Some(value.mkString(",")),
                    created_at = Some(new DateTime(DateTimeZone.UTC)),
                    updated_at = Some(new DateTime(DateTimeZone.UTC))
                  )
                )
              case Some(_) =>
                throw new IllegalStateException("Searching for an attribute and finding it seems to be illegal for some reason, but it's not clear why. If we're certain this shouldn't happen, should we bother searching and save ourselves a query?")
            }
          case None =>
            Logger.debug("[Event Reporting]: Unable to find specific attribute in table, " + attribute)
        }
    }
  }

  def withSessionIDAndNewEventID(f: => (Session, Event) => Request[AnyContent] => Result)(implicit environment: Environment):
  (Request[AnyContent] => Future[Result]) = { implicit request =>
    var (new_session, event) = create_persist_session_and_event(true,false)
    Future.successful(f(new_session, event)(request).withSession(new_session))
  }

  def withSessionIDAndCurrentEventID(f: => (Session, Event) => Request[AnyContent] => Result)(implicit environment: Environment):
  (Request[AnyContent] => Future[Result]) = { implicit request =>
    var (new_session, event) = create_persist_session_and_event(false,false)
    Future.successful(f(new_session, event)(request).withSession(new_session))
  }

  def withSessionIDAndSeparatePageRenderEventID(f: => (Session, Event) => Request[AnyContent] => Result)(implicit environment: Environment):
  (Request[AnyContent] => Future[Result]) = { implicit request =>
    var (new_session, event) = create_persist_session_and_event(true,true)
    Future.successful(f(new_session, event)(request).withSession(new_session))
  }

  def getTimeoutTs: DateTime = {
    new DateTime(DateTimeZone.UTC).plusMinutes(SESSION_TIMEOUT_MIN)
  }
}
