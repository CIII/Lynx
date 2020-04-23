package controllers

import java.net.URLEncoder
import javax.inject.Inject

import akka.actor.ActorSystem
import api.TokenAuthentication
import api.solar.calculator.{SolarCalculatorException, SolarCalculatorService}
import com.solarmosaic.client.mail.EnvelopeWrappers
import dao._
import listener.LeadpathEventListener
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import utils.EmailCommand

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import utils.utilities

class SolarCalculatorController @Inject()(
  val solarCalculatorService: SolarCalculatorService,
  implicit val environment: play.api.Environment,
  implicit val configuration: play.api.Configuration
) extends Controller with EnvelopeWrappers{

  def solarCalcEstimate = Action.async { implicit request =>

    val rawForm = request.body.asFormUrlEncoded.get
    val inputs = utilities.formatInputs(rawForm)

    try {
      val solar_calculation =
        Await.result(
          solarCalculatorService.get_solar_savings(
            inputs = inputs
          ),
          Duration.Inf
        )
      val solar_calculation_string = Json.prettyPrint(Json.toJson(solar_calculation))
      Future.successful(Ok(solar_calculation_string).as("application/json"))
    } catch {
      case sc: SolarCalculatorException =>
        Future.successful(BadRequest(sc.getMessage))
      case t: Throwable =>
        // Log error with message and Throwable.
        Logger.error("Exception with solar calculation", t)
        Future.successful(BadRequest("Bad solar calculation"))
    }
  }
}