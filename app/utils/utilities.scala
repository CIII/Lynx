package utils

import java.net.{URLDecoder, URLEncoder}
import java.util.Properties
import javax.mail.internet.InternetAddress

import api.solar.calculator.SolarCalculatorService
import api.solar.calculator.SolarCalculatorService._
import com.redis.RedisClient
import com.solarmosaic.client.mail.configuration.{PasswordAuthenticator, SmtpConfiguration}
import com.solarmosaic.client.mail.content.ContentType.MultipartTypes
import com.solarmosaic.client.mail.content.{Html, Multipart}
import com.solarmosaic.client.mail.{Envelope, EnvelopeWrappers, Mailer}
import dao.{EventDAO, UrlDAO}
import models.{Event, Url}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.data.Forms._
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.MapBuilder
import scala.concurrent.Promise
import play.api.libs.json.JsValue
import play.api.libs.json.JsResult
import play.api.libs.json.Reads
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.{Configuration, Play}
import play.api.mvc.AnyContent
import play.api.mvc.Request
import utils.templates.{SiteSettings, TemplateSettings}

import scala.util.matching.Regex
import play.api.Environment
import play.api.Logger

object utilities extends EnvelopeWrappers {

  val X_FORWARDED_FOR = "X-Forwarded-For";

  case class EasierSolarForm(
    var browser: models.Browser,
    var form: models.Form
  )

  lazy val EasierSolarFormMapping = play.api.data.Form(
    mapping(
      "browser" -> mapping(
        "id" -> ignored(Some(0L).asInstanceOf[Option[Long]]),
        "browser_id" -> optional(nonEmptyText),
        "created_at" -> ignored(None.asInstanceOf[Option[DateTime]]),
        "updated_at" -> ignored(None.asInstanceOf[Option[DateTime]])
      )(models.Browser.apply)(models.Browser.unapply),
      "form" -> mapping(
        "id" -> ignored(Some(0L).asInstanceOf[Option[Long]]),
        "browser_id" -> optional(longNumber),
        "event_id" -> optional(longNumber),
        "first_name" -> optional(text),
        "last_name" -> optional(text),
        "full_name" -> optional(text),
        "email" -> nonEmptyText,
        "street" -> optional(text),
        "city" -> optional(text),
        "state" -> optional(text),
        "zip" -> optional(text),
        "property_ownership" -> optional(text),
        "electric_bill" -> optional(text),
        "electric_company" -> optional(text),
        "phone_home" -> optional(text),
        "leadid_token" -> optional(text),
        "domtok" -> optional(text),
        "ref" -> optional(text),
        "xxTrustedFormCertUrl" -> optional(text),
        "xxTrustedFormToken" -> optional(text),
        "dob" -> optional(jodaDate("MM/dd/yyyy")),
        "post_status" -> ignored(None.asInstanceOf[Option[Int]])
      )(models.Form.apply)(models.Form.unapply)
    )(EasierSolarForm.apply)(EasierSolarForm.unapply)
  )

  def redis()(implicit configuration: play.api.Configuration) = new RedisClient(
    configuration.getString("redis.host").get,
    configuration.getInt("redis.port").get
  )

  def mailer()(implicit configuration: play.api.Configuration) = Mailer(
    SmtpConfiguration(
      "smtp.gmail.com",
      587,
      true,
      false,
      Some(
        PasswordAuthenticator(
          configuration.getString("email.username").get,
          configuration.getString("email.password").get
        )
      )
    )
  )

  lazy val SESSION_ID: String = "sid"
  lazy val PAGE_SERVED_ID: String = "ps_id"
  lazy val SESSION_TIMEOUT_ID: String = "session_timeout"
  lazy val SESSION_TIMEOUT_MIN: Int = 60
  lazy val UTM_SOURCE: String = "utm_source"
  lazy val UTM_CAMPAIGN: String = "utm_campaign"
  lazy val ADGROUPID: String = "adgroupid"
  lazy val UTM_CONTENT: String = "utm_content"
  lazy val DID: String = "did"

  /**
   * This method gets the domain for the relevant website. This is the full URL, not the slug for the domain. There is a
   * flag for whether this is for the use of cookies, because external sites request scripts through the URL tapxs.com,
   * so for the purposes of tracking the domain, the Referer should be used, but for the purpose of setting cookie
   * domains, "tapxs.com" should be used. For internal websites, this is not a distinction that is important.
   * 
   * The regex used in this method requires that the invariable, non-environment-specific portion of the URL has two
   * levels (something.com, something.pro, etc.)
   * 
   * @param forCookies		A flag that indicates whether this is going to be used for cookies. The default is that it is not used for cookies.
   * @param request				The HTTP request header for retrieving data from the request.
   * @return							A string of the domain name, with the removal of the environment specific portion.
   */
  def get_domain_name(forCookies: Boolean = false)(implicit request: RequestHeader) = {
    
    val configuration = TemplateSettings.TEMPLATE_CONFIGURATION
    request.host.contains("cloudfront.net") match {
      case true =>
        val cdnPattern = """\/(.+)\/static\/.*""".r
        request.path match {
          case cdnPattern(domain) =>
            configuration.getString("lynx.domains."+domain+".home_page.host") match {
              case Some(domain) => domain
              case _ => play.api.Logger.error(s"Matched domain, could not interpret URL ${request.host} for cdn, logging to default")
                "easiersolar.com"
            }
          case _ => play.api.Logger.error(s"Couldn't match domain, could not interpret URL ${request.host} for cdn, logging to default")
            "easiersolar.com"
        }
      case false =>
        val hostPattern = """(?:https?:\/\/)?(?:.*\.)*([a-zA-Z1-9\-]+\.[a-zA-Z1-9\-]+):*\d*\/?""".r
        val tapXsPattern = """(.*tapxs.com:?\d*)""".r
        var hostToMatch: Option[String] = None
        if (forCookies) {
          hostToMatch = Some(request.host)
        } else {
          request.host match {
            case tapXsPattern(_) => hostToMatch = request.headers.get("Referer")
            case _ => hostToMatch = Some(request.host)
          }
        }
        Logger.debug(s"hostToMatch: $hostToMatch")
        hostToMatch.fold({Logger.error("Did not find a Referer at tapxs.com"); "easiersolar.com"})({
          case hostPattern(domain) => domain
          case _ =>
            request.headers.get("User-Agent").map( userAgent =>
                if(!userAgent.contains("ELB")) play.api.Logger.error(s"Could not interpret URL ${hostToMatch.get}, logging to default"))
            "easiersolar.com"
        })
    }
  }

  def insert_event(session_id: Long, page_served_id: Option[Long], event_type_id: Long,
                   request_url: String, urlDAO: UrlDAO, eventDAO: EventDAO)
                  (implicit ec: ExecutionContext): Future[Event] = {

    for {
      url <- urlDAO.insert(
        Url(
          id = Some(0L),
          url = request_url,
          created_at = Some(new DateTime(DateTimeZone.UTC)),
          updated_at = Some(new DateTime(DateTimeZone.UTC))
        )
      )
      event <- eventDAO.insert(
        Event(
          id = Some(0L),
          event_type_id = Some(event_type_id),
          parent_event_id = page_served_id,
          session_id = Some(session_id),
          url_id = url.id,
          created_at = Some(new DateTime(DateTimeZone.UTC)),
          updated_at = Some(new DateTime(DateTimeZone.UTC))
        )
      )
    } yield (event)
  }

  def simplifyPhone(phone_number: String): String = {
    phone_number.replaceAll("[^\\d]", "")
  }

  def email_failed_post(rawForm: Map[String, Seq[String]], easierSolarForm: EasierSolarForm)(implicit configuration: play.api.Configuration): Unit = {
    mailer.send(Envelope(
      from = configuration.getString("email.from").get,
      to = configuration.getStringSeq("email.to").get.map(e => new InternetAddress(e)),
      subject = "Failed POST TapNexus",
      content = Multipart(
        parts = Seq(
          Html(
            List(
              "arrivalid=%s<br>",
              "ip=%s<br>",
              "first_name=%s<br>",
              "last_name=%s<br>",
              "email=%s<br>",
              "zip=%s<br>",
              "city=%s<br>",
              "state=%s<br>",
              "street=%s<br>",
              "property_ownership=%s<br>",
              "electric_bill=%s<br>",
              "electric_company=%s<br>",
              "phone_home=%s<br>",
              "leadid_token=%s<br>",
              "xxTrustedFormToken=%s<br>",
              "xxTrustedFormCertUrl=%s<br>",
              "listid=%s<br>",
              "domtok=%s<br>",
              "ref=%s"
            ).mkString.format(
              easierSolarForm.browser.browser_id,
              rawForm.getOrElse("ip", List("")).head,
              easierSolarForm.form.first_name,
              easierSolarForm.form.last_name,
              easierSolarForm.form.email,
              easierSolarForm.form.zip.getOrElse(""),
              easierSolarForm.form.city.getOrElse(""),
              easierSolarForm.form.state.getOrElse(""),
              easierSolarForm.form.street.getOrElse(""),
              easierSolarForm.form.property_ownership.getOrElse("RENT"),
              URLEncoder.encode(easierSolarForm.form.electric_bill.getOrElse("").replaceAll("&", "&amp;"), "UTF-8"),
              URLEncoder.encode(easierSolarForm.form.electric_company.getOrElse(""), "UTF-8"),
              simplifyPhone(easierSolarForm.form.phone_home.getOrElse("")),
              easierSolarForm.form.leadid_token.getOrElse(""),
              rawForm.getOrElse("xxTrustedFormToken", List("")).head,
              rawForm.getOrElse("xxTrustedFormCertUrl", List("")).head,
              rawForm.getOrElse("listid", List("")).head,
              easierSolarForm.form.domtok.getOrElse(""),
              easierSolarForm.form.ref.getOrElse("")
            )
          )
        ),
        subType = MultipartTypes.alternative
      )
    ))
  }

  def email_failed_ping(rawForm: Map[String, Seq[String]])(implicit configuration: play.api.Configuration): Unit = {
    mailer.send(Envelope(
      from = configuration.getString("email.from").get,
      to = configuration.getStringSeq("email.to").get.map(e => new InternetAddress(e)),
      subject = "Failed PING TapNexus",
      content = Multipart(
        parts = Seq(
          Html(
            List(
              "Failed to PING<br>",
              "ArrivalID: %s<br>",
              "LeadID Token: %s<br>",
              "ListID: %s<br>",
              "Domtok: %s<br>",
              "Form Ref: %s<br>",
              "IP: %s<br>",
              "First Name: %s<br>",
              "Last Name: %s<br>",
              "Email: %s<br>",
              "Zip: %s<br>",
              "City: %s<br>",
              "State: %s<br>",
              "Street: %s<br>"
            ).mkString.format(
              rawForm.getOrElse("arrival.browser_id", List("")).head,
              rawForm.getOrElse("form.leadid_token", List("")).head,
              rawForm.getOrElse("listid", List("")).head,
              rawForm.getOrElse("form.domtok", List("")).head,
              rawForm.getOrElse("form.ref", List("")).head,
              rawForm.getOrElse("ip", List("")).head,
              rawForm.getOrElse("form.first_name", List("")).head,
              rawForm.getOrElse("form.last_name", List("")).head,
              rawForm.getOrElse("form.email", List("")).head,
              rawForm.getOrElse("form.zip", List("")).head,
              rawForm.getOrElse("form.city", List("")).head,
              rawForm.getOrElse("form.state", List("")).head,
              rawForm.getOrElse("form.street", List("")).head,
              rawForm.getOrElse("form.property_ownership", List("RENT")).head,
              URLEncoder.encode(rawForm.getOrElse("form.electric_bill", List("")).head, "UTF-8"),
              URLEncoder.encode(rawForm.getOrElse("form.electric_company", List("")).head.replaceAll("&", "&amp;"), "UTF-8"),
              simplifyPhone(rawForm.getOrElse("form.phone_home", List("")).head),
              rawForm.getOrElse("xxTrustedFormToken", List("")).head,
              rawForm.getOrElse("xxTrustedFormCertUrl", List("")).head
            )
          )
        ),
        subType = MultipartTypes.alternative
      )
    ))
  }

  /**
    * This converts a map of futures to a future whose result is a map from the keys of the original Map to the results
    * of the Futures in the original Map. It is based on the Future.sequence source code (according to the author).
    */
  // This code from http://stackoverflow.com/questions/17479160/how-to-convert-mapa-futureb-to-futuremapa-b
  private def sequenceMap[A, B](in: Map[B, Future[A]]): Future[Map[B, A]] = {
    import play.api.libs.concurrent.Execution.Implicits._

    val mb = new MapBuilder[B, A, Map[B, A]](Map())
    in.foldLeft(Promise.successful(mb).future) {
      (fr, fa) => for (r <- fr; a <- fa._2.asInstanceOf[Future[A]]) yield (r += ((fa._1, a)))
    } map (_.result)
  }

  def parseQueryToMap(uri: String): scala.collection.mutable.Map[String, String] = {

    var params: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map()

    val parts = uri split "\\?"
    parts.length match {
      case l if l == 1 => {}
      case l if l == 2 => {
        val queryString = parts(1)
      }
      case _ => {}
    }
    parts.foreach { query =>
      query split "&" map { param =>
        val pair = param split "="
        val key = URLDecoder.decode(pair(0), "UTF-8")
        key match {
          case "browser_id" =>
          case _ =>
            pair.length match {
              case l if l > 1 =>
                val value = URLDecoder.decode(pair(1), "UTF-8")
                Logger.debug(s"Adding attribute ${key}: ${value}")
                params += key -> value
              case _ =>
            }
        }
      }
    }

    return params
  }

  def get_session_id_and_page_served_id(request: RequestHeader): (Option[Long], Option[Long]) = {
    //In case client deletes the cookie somehow
    val session_cookies = request.session
    (
      if (session_cookies.get(SESSION_ID).isEmpty) None else Some(session_cookies.get(SESSION_ID).get.toLong),
      if (session_cookies.get(PAGE_SERVED_ID).isEmpty) None else Some(session_cookies.get(PAGE_SERVED_ID).get.toLong))
  }

  def get_session_id_and_page_served_id(session_cookies: play.api.mvc.Session): (Option[Long], Option[Long]) = {

    (
      if (session_cookies.get(SESSION_ID).isEmpty) None else Some(session_cookies.get(SESSION_ID).get.toLong),
      if (session_cookies.get(PAGE_SERVED_ID).isEmpty) None else Some(session_cookies.get(PAGE_SERVED_ID).get.toLong))
  }

  def getClientIp(request: Request[AnyContent]): String = {

    //Case of http listener, ELB, this will forward
    request.headers.get(X_FORWARDED_FOR) match {
      case Some(ipAddress) =>
        //Sometimes x-forwarded-for includes several address
        if(ipAddress.contains(",")){
          return ipAddress.split(",")(0)
        }else{
          return ipAddress
        }
      case _ => // not available
    }

    request.body.asFormUrlEncoded match {
      case Some(body) =>
        body.get("ip_address") match {
          case Some(ipAddress) =>
            return ipAddress.head
          case _ => // not available in data
        }
      case _ => //not available in data
    }

    return request.remoteAddress
  }
  
  var cache_breaker: Option[String] = None
  var cdn_prefix: Option[String] = None
  
  def construct_url(urlBase: String,
                    queryParams: collection.mutable.Map[String, String] = collection.mutable.Map[String,String]())
                   (implicit configuration: Configuration, environment: Environment): String = {
    if (cache_breaker.isEmpty){
      val is = environment.classLoader.getResourceAsStream("cache_breaker.properties")
      val property: Properties = new Properties()
      property.load(is)
      cache_breaker = Some(property.getProperty("cache_breaker"))
    }
    if (cdn_prefix.isEmpty)cdn_prefix = configuration.getString("cloudfront.cdn.url")

    queryParams += "v" -> cache_breaker.get
    return s"${cdn_prefix.get}$urlBase?${queryParams.filter(_._2.nonEmpty).map {
      case (key, value) => s"$key=$value"
    }.mkString("&")}"
  }

  def construct_static_url(urlBase: String, siteSettings: SiteSettings,
                           queryParams: collection.mutable.Map[String, String] = collection.mutable.Map[String,String](),
                           isExternal:Boolean = false)
                          (implicit configuration: Configuration, environment: Environment): String = {
    import utils.templates.TemplateSettings._

    val directory = if(isExternal) "external" else get_directory(siteSettings)
    construct_url(s"${get_site_name(siteSettings)}/static/${directory}/${urlBase}", queryParams)
  }
  
  def formatInputs(rawForm: Map[String, Seq[String]]) = {
    val inputs = scala.collection.mutable.Map[String, Any]()
    import SolarCalculatorService._
    for ((key, values) <- rawForm) {
      key match {
        case "form.zip" => inputs += (ZIP_CODE -> values.head)
        case "form.electric_company" =>
          inputs += (ELECTRIC_COMPANY -> URLEncoder.encode(values.head.replaceAll("&", "&amp;"), "UTF-8"))
        case "form.state" => inputs += (STATE -> values.head)
        case "form.electric_bill" => {
          val value = URLDecoder.decode(values.head,"UTF-8").trim
          val power_bill_range_pattern = """\$*(\d+)-\$*(\d+)""".r
          val power_bill_single_pattern = """\$*(\d+)""".r
          var bill = 0.0
          try{
            val power_bill_range_pattern(min, max) = value
            bill = (min.toDouble + max.toDouble)/2.0
          }catch {
            case _ : Throwable =>
              val power_bill_single_pattern(power_bill) = value
              bill = power_bill.toDouble
          }
          inputs += (POWER_BILL -> bill)
        }
        case "form.kwh_price" => inputs += (KWH_PRICE -> values.head.toDouble)
        case "form.system_capacity" => inputs += (SYSTEM_CAPACITY -> values.head.toFloat)
        case "form.system_coverage" => inputs += (SYSTEM_COVERAGE -> values.head.toDouble)
        case "form.module_type" => inputs += (MODULE_TYPE -> values.head.toInt)
        case "form.array_type" => inputs += (ARRAY_TYPE -> values.head.toInt)
        case "form.tilt" => inputs += (TILT -> values.head.toFloat)
        case "form.azimuth" => inputs += (AZIMUTH -> values.head.toFloat)
        case "form.roof_shade" => inputs += (ROOF_SHADE -> values.head)
        case "form.loan_rate" => inputs += (LOAN_RATE -> values.head.toFloat)
        case "form.loan_period" => inputs += (LOAN_PERIOD -> values.head.toInt)
        case "form_progression" => inputs += (FORM_PROGRESSION -> values.head.toInt)
        case _ =>
      }
    }
    inputs.get(POWER_BILL) match {case None => inputs += (POWER_BILL -> 175.5) case _ =>}
    inputs
  }
}

// http://stackoverflow.com/questions/15488639/how-to-write-readst-and-writest-in-scala-enumeration-play-framework-2-1
object EnumUtils {
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = Reads[E#Value] {
    case JsString(s) => {
      try {
        JsSuccess(enum.withName(s))
      } catch {
        case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
      }
    }
    case _ => JsError("String value expected")
  }
}


