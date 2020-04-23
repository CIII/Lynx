package controllers

import java.net.URLDecoder
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import dao._
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import utils.utilities._

/** Defines a variety of constants and reference variables used to process events. Some of these references are loaded
 *  from the database when the [[EventController]] is first instantiated, and not when the static context is loaded, via
 *  the loadValues method. */
object  EventController {

  val EVENTS_REPORTED_SESSION_ATTRIBUTES = Seq(
    "ip_address",
    "os",
    "arpxs_a_ref",
    "arpxs_b",
    "arpxs_abv",
    "utm_source",
    "utm_campaign",
    "gclid",
    "referer",
    "robot_id",
    "maxmind_state",
    "maxmind_zip",
    "maxmind_country",
    "maxmind_city",
    "local_hour"
  )

  val EVENTS_REPORTED_ATTRIBUTES = Seq(
    "fire_error",
    "scroll_top",
    "scroll_bottom",
    "button_location",
    "button_text",
    "mouse_x",
    "mouse_y"
  )

  val EVENTS_REPORTED_PAGE_RENDER_ATTRIBUTES = Seq(
    "form_sequence"
  )

  var ATTRIBUTES = Map[String, Long]()
  var EVENT_TYPES = Map[String, Long]()
  val PAGE_LOADING = "page loading"
  val FORM_COMPLETE = "form complete"

  def loadValues(attributeDAO: AttributeDAO, eventTypeDAO: EventTypeDAO) = {

    (ATTRIBUTES.size, EVENT_TYPES.size) match {
      case (0, 0) =>
        attributeDAO.all().map {
          db_attributes =>
            db_attributes.map {
              attribute =>
                ATTRIBUTES += (attribute.name.toLowerCase -> attribute.id.get)
            }
        }

        eventTypeDAO.all().map {
          db_eventTypes =>
            db_eventTypes.map {
              eventType =>
                EVENT_TYPES += (eventType.name.toLowerCase -> eventType.id.get)
            }
        }
      case _ =>
    }
  }
}

class EventController @Inject()(
 val browserDAO: BrowserDAO,
 val urlDAO: UrlDAO,
 val attributeDAO: AttributeDAO,
 val eventDAO: EventDAO,
 val eventAttributeDAO: EventAttributeDAO,
 val eventTypeDAO: EventTypeDAO,
 val formDAO: FormDAO,
 val sessionDAO: SessionDAO,
 val sessionAttributeDAO: SessionAttributeDAO,
 val ws: WSClient,
 val messagesApi: MessagesApi,
 implicit val system: ActorSystem,
 implicit val materializer: Materializer,
 implicit val environment: play.api.Environment,
 implicit val configuration: play.api.Configuration
) extends Controller with I18nSupport {

  import EventController._
  loadValues(attributeDAO, eventTypeDAO)
  if(ATTRIBUTES.size <= 0) Logger.error("[Events Reporting] Attributes not loaded")
  if(EVENT_TYPES.size <= 0) Logger.error("[Events Reporting] Event Types not loaded")

  /** Defines an endpoint for the browser to report an event of any type. It is not clear at this time why the
   *  page_loading event has its own endpoint. **/
  def create = Action.async { implicit request =>
    val data:Map[String, Seq[String]] = request.body.asFormUrlEncoded.get
    val browser_id = data("browser_id").head
    val event = data("event").head
    val request_url = data.getOrElse("request_url", Seq()).headOption

    val (session_attributes_map, event_attributes_map, page_rendered_event_attributes_map):
      (Map[String, String], Map[String, String], Map[String, String]) = {

      var session_params : Map[String, String] = Map()
      var event_params : Map[String, String] = Map()
      var page_rendered_event_params: Map[String, String] = Map()

      def populate_uri_attributes(uri: String): Unit = {
        val queryStringRe = """^.*\?(.*)#.*$""".r
        val queryString = queryStringRe.findFirstMatchIn(uri.replace("&amp;", "&"))
        queryString.map { x =>
          if(x.groupCount > 0) {
            Logger.debug("Match: " + x.group(1))
            val pairsRe = """([^&]|&amp;)+""".r
            val pairs = for(attrPair <- pairsRe.findAllIn(x.group(1))) {
              Logger.debug("Pair: " + attrPair)
              val pair = attrPair split "="
              val key = URLDecoder.decode(pair(0), "UTF-8")
              key match {
                case "browser_id" =>
                case _ =>
                  pair.length match {
                    case l if l > 1 =>
                      if(EVENTS_REPORTED_SESSION_ATTRIBUTES.contains(key)) {
                        session_params += key -> URLDecoder.decode(pair(1), "UTF-8")
                      } else if(EVENTS_REPORTED_ATTRIBUTES.contains(key)){
                        event_params += key -> URLDecoder.decode(pair(1), "UTF-8")
                      } else if (EVENTS_REPORTED_PAGE_RENDER_ATTRIBUTES.contains(key)){
                        page_rendered_event_params += key -> URLDecoder.decode(pair(1), "UTF-8")
                      }
                    case _ =>
                  }
              }
            }
          }
        }
      }

      def populate_body_attributes(): Unit = {
        data.map {
          case (key, value) =>
            if(EventController.EVENTS_REPORTED_SESSION_ATTRIBUTES.contains(key) && value.length > 0){
              session_params += key -> value(0)
            }else if(EventController.EVENTS_REPORTED_ATTRIBUTES.contains(key) && value.length > 0){
              event_params += key -> value(0)
            }else if(EventController.EVENTS_REPORTED_PAGE_RENDER_ATTRIBUTES.contains(key)){
              page_rendered_event_params += key -> value(0)
            }
        }
      }

      populate_uri_attributes(request_url.get)
      populate_body_attributes()
      session_params += "ip_address" -> utils.utilities.getClientIp(request)
      (session_params, event_params, page_rendered_event_params)
    }

    val (sid, ps_id) = utils.utilities.get_session_id_and_page_served_id(request)

    def update_session_is_robot(session: Session, robot_id: Option[String]) = {
      robot_id match {
        case Some(robotId) =>
          session.is_robot = true
          sessionDAO.update(session)
        case None => // Do not update if previously false, and now there's no robot id
      }
    }

    def update_session_ip_address(session: Session, ip_address: Option[String]) = {
      ip_address match {
        case Some(ipAddress) =>
          //If there is an ip address and different from current one
          if(session.ip.getOrElse("") != ipAddress) {
            session.ip = Some(ipAddress)
            sessionDAO.update(session)
          }
        case None => //Do not update
      }
    }

    val current_time = Some(new DateTime(DateTimeZone.UTC))

    ps_id match {
      case None =>
        Logger.error(s"No parent event id found, could not persist page served attributes (${page_rendered_event_attributes_map.toString})")
      case Some(ps_id) =>
        page_rendered_event_attributes_map.foreach {
          case (attribute, value) =>
            ATTRIBUTES.get(attribute.toLowerCase) match {
              case Some(id) =>
                eventAttributeDAO.findByEventIdAndAttribute(ps_id,id).map{
                  case Some(eventAttribute) =>
                    eventAttribute.value = Some(value)
                    eventAttribute.updated_at = current_time
                    eventAttributeDAO.update(
                      eventAttribute
                    )
                  case _ =>
                    eventAttributeDAO.insert(
                      EventAttribute(
                        id = Some(0L),
                        event_id = Some(ps_id),
                        attribute_id = Some(id),
                        value = Some(value),
                        created_at = current_time,
                        updated_at= current_time
                      )
                    )
                }
              case _ =>
            }
          case _ =>
        }
    }

    sid match {
      case None =>
        Logger.error(s"No session id found, could not create event (${data})")
      case Some(sid) =>
        sessionDAO.find(sid).map {
          case Some(session) =>
            // Find browser, if found and session doesn't have browser_id, update
            browserDAO.findByBrowserId(browser_id).map {
              case Some(browser)=>
                if(session.browser_id.isEmpty) session.browser_id = browser.id
                sessionDAO.update(session)
              case None =>
                Logger.error(s"Error Browser Not Found for BrowserId: $browser_id")
            }
            //update is_robot if there is a robot id
            update_session_is_robot(session, session_attributes_map.get("robot_id"))
            //update ip_address
            update_session_ip_address(session, session_attributes_map.get("ip_address"))

            //populate each session attribute
            session_attributes_map.foreach {
              case(attribute, value) =>
                ATTRIBUTES.get(attribute.toLowerCase) match {
                  case Some(id) =>
                    sessionAttributeDAO.updateValueIfExists(sid, id, value)
                  case None =>
                }
            }

            EVENT_TYPES.get(event.toLowerCase) match {
              case Some(id) =>
                if (id != EVENT_TYPES(FORM_COMPLETE)) {
                  if(ps_id.isEmpty) Logger.error(s"No parent id found, event (${data})")
                  insert_event(sid, ps_id, id, request_url.get, urlDAO, eventDAO).map {
                    case event: Event =>
                      event_attributes_map.foreach {
                        case (attribute, value) =>
                          ATTRIBUTES.get(attribute.toLowerCase) match {
                            case Some(id) =>
                              eventAttributeDAO.insert(
                                EventAttribute(
                                  id = Some(0L),
                                  event_id = event.id,
                                  attribute_id = Some(id),
                                  value = Some(value),
                                  created_at = current_time,
                                  updated_at= current_time
                                )
                              )
                            case _ =>
                          }
                        case _ =>
                      }
                    case _ =>
                      Logger.error(s"[Events Reporting] EVENT not made: ${sid.toString} |${ps_id.toString} | ${request_url.get}")
                  }
                }
              case _ =>
                Logger.error(s"[Events Reporting] event type not found: ${event}")
            }
          case None =>
            Logger.error(s"[Events Reporting] session not found: ${sid.toString}")
        }
    }

    Future.successful(Ok("success"))
  }

  /** Defines an endpoint for the browser to report the page_loading event. This is fired in the Angular service method 
   *  CommonService#eventCreate */
  def page_loading = Action.async {
    implicit request =>
      val (sid, ps_id) = utils.utilities.get_session_id_and_page_served_id(request)
      val url = request.host + request.uri
      val current_time = Some(new DateTime(DateTimeZone.UTC))

      urlDAO.insert(
        Url(
          id = Some(0L),
          url = url,
          created_at = current_time,
          updated_at = current_time
        )
      ).map {
        url =>
          val newEvent = Event(
            id = Some(0L),
            event_type_id= EVENT_TYPES.get(PAGE_LOADING),
            session_id = sid,
            parent_event_id = ps_id,
            url_id = url.id,
            created_at = current_time,
            updated_at = current_time
          )
          eventDAO.insert(newEvent)
        (sid, ps_id) match {
          case (Some(session_id), Some(parent_event_id)) =>
          case _ =>
            Logger.error(s"[Event Reporting] Missing either session: $sid or parent event: $ps_id, cannot fully persist ${newEvent.toString}")
        }
      }


      Future.successful(Ok("\"Page loading received\""))
  }
}
