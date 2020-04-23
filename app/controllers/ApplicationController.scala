package controllers

import java.util.UUID
import javax.inject.Inject

import com.google.i18n.phonenumbers.{NumberParseException, PhoneNumberUtil}
import dao._
import models.Browser
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/** Defines a few utility endpoints. */
class ApplicationController @Inject()(
  val browserDAO: BrowserDAO,
  val urlDAO: UrlDAO,
  val eventDAO: EventDAO,
  val eventTypeDAO: EventTypeDAO,
  val formDAO: FormDAO,
  val ws: WSClient,
  val messagesApi: MessagesApi,
  implicit val environment: play.api.Environment,
  implicit val configuration: play.api.Configuration
) extends Controller with I18nSupport {
  /** Validates a phone number against Google's phone number service. */
  def validate_phone(phone: String) = Action { implicit request =>
    if(phone != "5127637491") {
      val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
      var isValid = false
      try {
        val numberProto = phoneUtil.parse(phone, "US")
        isValid = phoneUtil.isValidNumber(numberProto)
      } catch {
        case npe: NumberParseException =>
          Logger.debug("NumberParseException was thrown: " + npe.toString)
      }
      if(isValid)
        Ok("success")
      else
        Forbidden
    } else {
      Ok("ruxit success")
    }
  }

  /** An endpoint that starts a new session for the browser. */
  def new_session() = Action.async { implicit request =>
    Future.successful(Ok("").withNewSession)
  }

  /** Returns a JavaScript file that initializes the browser with the browser_id, which is used to identify inidividual
   *  browsers across sessions. */
  def browser_id() = Action.async { implicit request =>
    var browserId = ""
    Future.successful(
      Ok("""
        var qs = (function(a) {
            if (a == "") return {};
            var b = {};
            for (var i = 0; i < a.length; ++i)
            {
                var p=a[i].split('=', 2);
                if (p.length == 1)
                    b[p[0]] = "";
                else
                    b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
            }
            return b;
        })(window.location.search.substr(1).split('&'));
        if(qs["browser_id"] !== undefined) {
          var browserId = qs["browser_id"];
        } else {
          var browserId = '%s';
        }
        localStorage.setItem("browser_id", browserId);
        console.log('browserId -->', browserId);
      """.stripMargin.format(
        {
          browserId = (request.cookies.get("browser_id") match {
            case Some(cookie) =>
              cookie.value
            case None =>
              request.cookies.get("arrival_id") match {
                case Some(cookie) =>
                  cookie.value
                case None =>
                  Await.result (
                    browserDAO.insert (
                      Browser (
                        Some (0L),
                        Some (UUID.randomUUID.toString),
                        Some (new DateTime (DateTimeZone.UTC) ),
                        Some (new DateTime (DateTimeZone.UTC) )
                      )
                    ),
                    Duration.Inf
                  ).browser_id.getOrElse (throw new Exception ("Error Creating Browser Record!") )
              }
          }).toString
          browserId
        }
      )).withHeaders(CONTENT_TYPE -> "text/javascript").withCookies(Cookie("browser_id", browserId))
    )
  }
}
