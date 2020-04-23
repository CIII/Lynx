package controllers

import java.net.URLEncoder
import java.text.DecimalFormat
import javax.inject.Inject
import javax.mail.internet.InternetAddress

import actors.LeadInfoActor
import actors.Leadpath._
import akka.actor.{ActorSelection, ActorSystem, InvalidActorNameException}
import api.TokenAuthentication
import api.solar.calculator.SolarCalculatorService
import com.solarmosaic.client.mail.content.ContentType.MultipartTypes
import com.solarmosaic.client.mail.content.{Html, Multipart}
import com.solarmosaic.client.mail.{Envelope, EnvelopeWrappers}
import dao._
import listener.LeadpathEventListener
import models.{Event, Form, Revenue}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import utils.calculator.SolarCalculator
import utils.{EmailCommand, EmailCommandData, utilities}
import utils.utilities._

import scala.collection.JavaConversions
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

/** Provides the endpoints for reporting user form data, which is then forwarded to Leadpath. */
class LeadPathController @Inject()(
  val browserDAO: BrowserDAO,
  implicit val apiTokenDAO: ApiTokenDAO,
  val sessionDAO: SessionDAO,
  val revenueDAO: RevenueDAO,
  val urlDAO: UrlDAO,
  val eventDAO: EventDAO,
  val eventTypeDAO: EventTypeDAO,
  val formDAO: FormDAO,
  implicit val ws: WSClient,
  val messagesApi: MessagesApi,
  val listingsDAO: ListingsDAO,
  implicit val system: ActorSystem,
  val solarCalculatorService: SolarCalculatorService,
  val listeners: java.util.Set[LeadpathEventListener],
  val emailCommand: EmailCommand,
  val whitepagesDAO: WhitepagesDAO,
  val leadDAO: LeadDAO,
  implicit val environment: play.api.Environment,
  implicit val configuration: play.api.Configuration
)(
    implicit val sessionAttributeDAO: SessionAttributeDAO = null,
    implicit val attributeDAO: AttributeDAO = null) extends Controller with EnvelopeWrappers with I18nSupport with TokenAuthentication {

  LeadPathController.loadValues
  implicit val RevenueFormat = Json.format[Revenue]

  def ping_form(domain: String) = Action.async { implicit request =>

    ping_leadpath(request, domain)

    val rawForm = request.body.asFormUrlEncoded.get

    Future.successful(Ok)
  }

  def ping_leadpath(implicit request: Request[AnyContent], domain: String): Future[Unit] = {
    try {
      val rawForm = request.body.asFormUrlEncoded.get
      val (sid, _) = utils.utilities.get_session_id_and_page_served_id(request)
      Logger.debug(s"[Leadpath Controller] From client, ping ${rawForm.toString} for session ${sid.toString}")
      val session_id = sid match {case Some(session_id) => session_id case None => 0L}
      val parameters = constructParametersFromForm(session_id, ping_accessors(rawForm))
      for {
        attributesParameters <- LeadPathController.constructParametersFromAttributes(session_id)
        response <- {
          val fullParameters = parameters ++ attributesParameters
          Logger.debug("Ping Body -> %s".format(fullParameters.toString))
          val response = ws.url(configuration.getString("leadpost.ping.uri").getOrElse(throw new Exception("Missing Leadpost Ping URI"))).
              withFollowRedirects(true).
              withHeaders("Content-Type" -> "application/x-www-form-urlencoded").
              withRequestTimeout(15 seconds).
            post(fullParameters)
          response.recover {
            case e: Exception => {
              Logger.error("Exception sending PING to TapNexus.", e)
              email_failed_ping(rawForm)
              throw e
          }}
        }
        httpResponse <- {
          Future.successful {
            if (response.status != 200) {
              Logger.error(
                "Failed to PING %s to TapNexus. Response: <pre>%s</pre>".format(
                  rawForm.getOrElse("arrival.browser_id", List("")).head,
                  response.body))
              email_failed_ping(rawForm)
            }
            // If successful create response in leads table
            try {
              val lead_id = (response.json \ "lead" \ "id").as[String].toLong
              leadDAO.createIfNotExist(lead_id, session_id)
            }catch{
              case e: Exception =>
                Logger.error(s"Unable to create lead record: ${e.getMessage}")
            }
            Logger.debug("Ping Response ->%s".format(response.body))
          }
        }
      } yield httpResponse
    } catch {
      case e: Exception =>
        Future.successful {
          Logger.error(s"Error sending a ping: ${e.getMessage}:\n\n${e.getStackTrace.mkString("\n\t")}")
          try {
            email_alert(Some(e))
          } catch {
          case ee: Exception => {
            Logger.error(s"Failure to send an email on error. Failing silently.\nCaught Error: ${e.getMessage}\nMail Error: ${ee.getMessage}")
          }
        }
      }
    }
  }

  def post_form(domain: String) = Action.async { implicit request =>
    val response = perform_post(request, domain)
    Future.successful(Ok(views.html.thank_you()))
  }
  
  def post_form_api(domain: String) = Action.async { implicit request =>
    perform_post(request, domain)
  }

  private def email_alert(exception: Option[Exception] = None)(implicit request: Request[AnyContent]) = {

    val content = exception match {
      case Some(e) =>
        Multipart(
          parts = Seq(
            Html(
              List("Post Exception: <br>", "%s <br>", "Form submitted: <br>", "%s").mkString.format(
                e.getStackTrace.mkString("<br>"), request.body.asFormUrlEncoded.toString))),
          subType = MultipartTypes.alternative)
      case _ =>
        Multipart(
          parts = Seq(
            Html(
              List("Post Exception: <br>", "form error <br>", "Form submitted: <br>", "%s").mkString.format(
                request.body.asFormUrlEncoded.toString))),
          subType = MultipartTypes.alternative)
    }

    mailer.send(Envelope(
      from = configuration.getString("email.from").get,
      to = configuration.getStringSeq("email.to").get.map(e => new InternetAddress(e)),
      subject = "Failed Post TapNexus",
      content = content))
  }
  
  private def perform_post(implicit request: Request[AnyContent], domain: String) = {
    val (sid, ps_id) = utils.utilities.get_session_id_and_page_served_id(request)
    var post_status: Option[Int] = None
    try {
      val rawForm = request.body.asFormUrlEncoded.get
      Logger.debug(s"[Leadpath Controller] From client, post ${rawForm.toString} for session ${sid.toString}")
      EasierSolarFormMapping.bindFromRequest.fold(
        errorForm => {
          Logger.error("Error occurred! -> %s".format(errorForm.toString))
          try {
            email_alert()
          }catch{
            case ee: Exception =>
              Logger.error(s"Failure to send an email on error. Failing silently.\nMail Error: ${ee.getMessage}")
          }
          Future.successful(BadRequest("Post Error"))
        },
        easierSolarForm => {
          if(easierSolarForm.form.electric_bill.isEmpty ||
              rawForm.get("ip").isEmpty) { // This value is actually required, but it is marked optional in the form definition, presumably because forms can be partially submitted as part of the ping process. I am not really certain what to do about this situation, but more than a NullPointerException is required.
            throw new IllegalArgumentException("Electric bill and city are required to post the form.")
          }
          /*
          if(easierSolarForm.form.property_ownership.isEmpty || (easierSolarForm.form.property_ownership.get != "OWN" && easierSolarForm.form.property_ownership.get != "RENT")) {
            Logger.error("Property Ownership must be 'OWN' or 'RENT'")
            throw new IllegalArgumentException("Property Ownership must be 'OWN' or 'RENT'")
          }*/
          
          // The following 3 items do not need to block on each other and so do not need to be in one long monad. They
          //  also do not all need to be blocked on before returning a result; only postDataToLeadpath needs to finish
          //  before posting a response.
          val session_id = sid match {case Some(session_id) => session_id case None => 0L}
          val page_serve_id = ps_id match{case Some(page_serve_id) => page_serve_id case None => 0L}
          notifyUserByEmail(easierSolarForm, rawForm, domain)
          for {
            storedFormData <- storePostFormData(easierSolarForm, rawForm, session_id, page_serve_id)
            postData <- {
              easierSolarForm.form = storedFormData
              postDataToLeadpathAndProcess(easierSolarForm, rawForm, session_id)(request=request, domain=domain)
            }
          } yield postData
        })
    } catch {
      case e: Exception =>
        try {
          email_alert(Some(e))
        }catch{
          case ee: Exception =>
            Logger.error(s"Failure to send an email on error. Failing silently.\nCaught Error: ${e.getMessage}\nMail Error: ${ee.getMessage}")
        }
        Future.successful(BadRequest("Post Error"))
    }
  }

  /**
   * @return
   */
  def post_disposition = withApiToken {
    token =>
      { request =>
        Logger.debug(s"Starting serving post: ${request.body}")
        val leadpath_message = request.body.asJson.get
        Logger.debug(s"JSON: ${leadpath_message.toString()}")

        Logger.debug("Validating message...")
        try {
          leadpath_message.validate[LeadpathArrival].fold(
            { errors =>
              Logger.error("[Leadpath Reporting]: Improper session id value. " + errors.toString())
              BadRequest("[Leadpath Reporting]: Improper session id value. " + errors.toString())
            },
            { leadpathArrival: LeadpathArrival =>
              Logger.debug("Validated message")
              val listenerFutures = Future.sequence(JavaConversions.asScalaSet(listeners).map(f =>
                f.processMessage(leadpathArrival)))
              Logger.debug("Past listeners")
              Await.result(listenerFutures.map(results => Ok("Result")), Duration.Inf) // TODO: This does not include the resuts from the summation, but an event shouldn't expect to return a result, and Leadpath is publishing an event.
            })
        } catch {
          case e: Throwable => {
            Logger.error("Unknown error here: " + e.getStackTrace.mkString("\n"))
            throw e
          }
        }

      }
  }
  
  def postDataToLeadpath(easierSolarForm: EasierSolarForm, rawForm: Map[String, Seq[String]], sid: Long)(implicit request: Request[AnyContent], domain: String): Future[WSResponse] = {
    val formParameters = constructParametersFromForm(sid, post_accessors(rawForm, easierSolarForm))(request=request, domain=domain)
    Logger.debug("calling ws")
    val response = for {
      attributesResult <- {
        val attributesResult = LeadPathController.constructParametersFromAttributes(sid)
        attributesResult.onFailure({ case e => Logger.error("Error retrieving attributes for post to Leadpath.", e) })
        attributesResult
      }
      postResults <- {
        var parameters = formParameters
        parameters = parameters ++ attributesResult
        Logger.debug("Parameters: " + parameters.toString())
        val returnValue = ws.url(configuration.getString("leadpost.post.uri").getOrElse(throw new Exception("Missing Leadpost Post URI"))).
            withFollowRedirects(true).
            withHeaders("Content-Type" -> "application/x-www-form-urlencoded").
            withRequestTimeout(15 seconds).
            post(parameters)
        returnValue.onFailure {
          case ex: Throwable => { Logger.error("Failure during request to Leadpath: ", ex) }
        }
        returnValue
      }
    } yield(postResults)
    response
  }
  
  private def postDataToLeadpathAndProcess(easierSolarForm: EasierSolarForm, rawForm: Map[String, Seq[String]], sid: Long)(implicit request: Request[AnyContent], domain: String): Future[Result] = {
    Logger.debug("calling ws")
    val response = for {
      postResults <- postDataToLeadpath(easierSolarForm, rawForm, sid)(request=request, domain=domain)
      responseProcessing <- {
        if (postResults.status != 200) {
          Logger.error("Failed to POST %s to as-leads. Response: <pre>%s</pre>".format(easierSolarForm.browser.browser_id, postResults.body))
          email_failed_post(rawForm, easierSolarForm)
        }
        // This assumes that we never pass in WAIT_FOR_RESPONSE or WAIT_FOR_HTTP_RESPONSE or whatever it is.
        // This has a potential race condition if Leadpath finishes processing and publishes the event to post_disposition before we get here. Remember to put a pull in the LeadInfoBridgeActor in case the WebSocket is set up before the response comes back.
        Logger.debug(s"Response from Leadpath: ${postResults.body}")

        easierSolarForm.form.post_status = Some(postResults.status)
        formDAO.update(easierSolarForm.form)

        Future{ Json.parse(postResults.body).validate[LeadpathArrival].fold(
          { errors =>
            Logger.warn("Error parsing the response from Leadpath. Will not create the actor to receive messages from the client regarding listings. " + errors.toString())
            InternalServerError("Error parsing the response from Leadpath.")
          },
          { leadpathArrival =>
            Logger.debug("creating actor")
            LeadPathController.createWebsocketActor(None,Some(leadpathArrival)) match {
              case (Some(actor), true) =>
                val listenerFutures = Future.sequence(JavaConversions.asScalaSet(listeners).map(f =>
                  f.processMessage(leadpathArrival)))
                Logger.debug("Past listeners")
              case _ =>
            }

            Logger.debug("So far so good")
            Logger.debug(s"${leadpathArrival.lead.id}")
            Ok(s"""{"lead_id": "${leadpathArrival.lead.id}"}""").as("application/json")
        }) }
      }
    } yield (responseProcessing)
    response.onFailure{
      case e => {
        Logger.debug("An error was thrown sending the email.", e)
      }
    }
    response
  }
  
  private def notifyUserByEmail(easierSolarForm: EasierSolarForm, rawForm: Map[String, Seq[String]], domain: String): Future[Unit] = {
    for {
      solarFormCalculation <- {
        Logger.debug("Getting solar calculations")
        val inputs = utilities.formatInputs(rawForm)
        val solarSavings = solarCalculatorService.get_solar_savings(inputs)
        solarSavings.onFailure {
          case e => Logger.warn("Error getting solar savings.", e)
        }
        solarSavings
      }
      emailSend <- {
        Logger.debug("Sending email")
        val format = new DecimalFormat("$#,##0.00;-$#,##0.00")
        val powerFormat = new DecimalFormat("#,##0.00;-#,##0.00")
        val data = EmailCommandData(
            easierSolarForm.form.email,
            easierSolarForm.form.first_name.getOrElse(""),
            easierSolarForm.form.last_name.getOrElse(""),
            easierSolarForm.form.city,
            easierSolarForm.form.electric_bill.get,
            powerFormat.format(solarFormCalculation.inputs.kwh_usage),
            powerFormat.format(solarFormCalculation.outputs.proposed_system.system_size),
            format.format(solarFormCalculation.outputs.loan.savings_total.getOrElse(throw new Exception("Failed to find loan savings_total in solar form."))),
            format.format(solarFormCalculation.outputs.loan.monthly_cost.getOrElse(throw new Exception("Failed to find loan monthly_cost in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.monthly_cost.getOrElse(throw new Exception("Failed to find ppa monthly_cost in solar form."))),
            format.format(solarFormCalculation.outputs.cash.monthly_cost.getOrElse(throw new Exception("Failed to find cash monthly_cost in solar form."))),
            format.format(solarFormCalculation.outputs.loan.system_payment.getOrElse(throw new Exception("Failed to find loan system_payment in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.system_payment.getOrElse(throw new Exception("Failed to find ppa system_payment in solar form."))),
            format.format(solarFormCalculation.outputs.cash.system_payment.getOrElse(throw new Exception("Failed to find cash system_payment in solar form."))),
            format.format(solarFormCalculation.outputs.loan.srec_credits.getOrElse(throw new Exception("Failed to find loan srec_credits in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.srec_credits.getOrElse(throw new Exception("Failed to find ppa srec_credits in solar form."))),
            format.format(solarFormCalculation.outputs.cash.srec_credits.getOrElse(throw new Exception("Failed to find cash srec_credits in solar form."))),
            format.format(solarFormCalculation.outputs.loan.extra_grid_power.getOrElse(throw new Exception("Failed to find loan extra_grid_power in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.extra_grid_power.getOrElse(throw new Exception("Failed to find ppa extra_grid_power in solar form."))),
            format.format(solarFormCalculation.outputs.cash.extra_grid_power.getOrElse(throw new Exception("Failed to find cash extra_grid_power in solar form."))),
            format.format(solarFormCalculation.outputs.loan.net_monthly_savings.getOrElse(throw new Exception("Failed to find loan net_monthly_savings in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.net_monthly_savings.getOrElse(throw new Exception("Failed to find ppa net_monthly_savings in solar form."))),
            format.format(solarFormCalculation.outputs.cash.net_monthly_savings.getOrElse(throw new Exception("Failed to find cash net_monthly_savings in solar form."))),
            format.format(solarFormCalculation.outputs.loan.year_one_savings_percent.getOrElse(throw new Exception("Failed to find loan year_one_savings_percent in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.year_one_savings_percent.getOrElse(throw new Exception("Failed to find ppa year_one_savings_percent in solar form."))),
            format.format(solarFormCalculation.outputs.cash.year_one_savings_percent.getOrElse(throw new Exception("Failed to find cash year_one_savings_percent in solar form."))),
            format.format(solarFormCalculation.outputs.loan.savings_total.getOrElse(throw new Exception("Failed to find loan savings_total in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.savings_total.getOrElse(throw new Exception("Failed to find ppa savings_total in solar form."))),
            format.format(solarFormCalculation.outputs.cash.savings_total.getOrElse(throw new Exception("Failed to find cash savings_total in solar form."))),
            format.format(solarFormCalculation.outputs.loan.upfront_cost.getOrElse(throw new Exception("Failed to find loan upfront_cost in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.upfront_cost.getOrElse(throw new Exception("Failed to find ppa upfront_cost in solar form."))),
            format.format(solarFormCalculation.outputs.cash.upfront_cost.getOrElse(throw new Exception("Failed to find cash upfront_cost in solar form."))),
            format.format(solarFormCalculation.outputs.loan.effective_price.getOrElse(throw new Exception("Failed to find loan effective_price in solar form."))),
            format.format(solarFormCalculation.outputs.ppa.effective_price.getOrElse(throw new Exception("Failed to find ppa effective_price in solar form."))),
            format.format(solarFormCalculation.outputs.cash.effective_price.getOrElse(throw new Exception("Failed to find cash effective_price in solar form."))),
            f"${solarFormCalculation.outputs.loan.savings_percentage.getOrElse(throw new Exception("Failed to find loan savings_percentage in solar form calculation.")) * 100}%1.2f%%",
            f"${solarFormCalculation.outputs.ppa.savings_percentage.getOrElse(throw new Exception("Failed to find ppa savings_percentage in solar form calculation.")) * 100}%1.2f%%",
            f"${solarFormCalculation.outputs.cash.savings_percentage.getOrElse(throw new Exception("Failed to find cash savings_percentage in solar form calculation.")) * 100}%1.2f%%")
        val emailSend = Future {
          emailCommand.sendEmail(data, domain)
          Logger.debug("Sent user email")
        }
        emailSend.onFailure{
          case e => {
            Logger.warn("Error sending email to Mandrill.", e)
          }
        }
        emailSend
      }
    } yield (emailSend)
  }
  
  private def storePostFormData(easierSolarForm: EasierSolarForm, rawForm: Map[String, Seq[String]], sid: Long, ps_id: Long)(implicit request: Request[AnyContent]) : Future[Form] = {
    for {
      event <- eventTypeDAO.findByEventName("Form Complete")
      updatedEvent: Event <- { event match {
          case Some(eventType) => insert_event(sid, Some(ps_id), eventType.id.get, request.host + request.uri, urlDAO, eventDAO)
          case _ => Future(null)
        }
      }
      eventData <- {
        easierSolarForm.form.post_status = None
        easierSolarForm.form.session_id = Some(sid)
        easierSolarForm.form.event_id = updatedEvent.id
        easierSolarForm.form.xxTrustedFormToken = Some(rawForm.getOrElse("xxTrustedFormToken", List("")).head)
        easierSolarForm.form.xxTrustedFormCertUrl = Some(rawForm.getOrElse("xxTrustedFormCertUrl", List("")).head)
        formDAO.insert(easierSolarForm.form)
      }
    } yield (eventData)
  }
  
  private def constructParametersFromForm(sid: Long, accessors: AccessorMap)(implicit request: RequestHeader, domain: String): Map[String, Seq[String]] = {
    val parameterNames = Seq(
      "arrivalid",
      "credit_range",
      "ip",
      "ua",
      "first_name",
      "last_name",
      "full_name",
      "email",
      "zip",
      "city",
      "state",
      "street",
      "property_ownership",
      "electric_bill",
      "electric_company",
      "phone_home",
      "leadid_token",
      "xxTrustedFormToken",
      "xxTrustedFormCertUrl",
      "listid",
      "domtok",
      "ref",
      "robot_id",
      "local_hour",
      "asid",
      "dob")

    val parameterValues = Seq(
      sid.toString,
      accessors.get("credit_range").get(""),
      accessors.get("ip").get(""),
      accessors.get("User-Agent").get(""),
      accessors.get("first_name").get(""),
      accessors.get("last_name").get(""),
      accessors.get("full_name").get(""),
      accessors.get("email").get(""),
      accessors.get("zip").get(""),
      accessors.get("city").get(""),
      accessors.get("state").get(""),
      accessors.get("street").get(""),
      accessors.get("property_ownership").get(""),
      accessors.get("electric_bill").get(""),
      accessors.get("electric_company").get(""),
      accessors.get("phone_home").get(""),
      accessors.get("leadid_token").get(""),
      accessors.get("xxTrustedFormToken").get(""),
      accessors.get("xxTrustedFormCertUrl").get(""),
      accessors.get("listid").get("solar_full_form"),
      accessors.get("domtok").get("sunnyS0lar"),
      accessors.get("ref").get("www.easiersolar.com"),
      accessors.get("robot_id").get(""),
      accessors.get("local_hour").get(""),
      accessors.get("asid").get("www.easiersolar.com"),
      accessors.get("dob").get("")).map {
      Seq(_)
    }

    (parameterNames zip parameterValues).toMap
  }
  
  import scala.reflect.runtime.universe._
  val rawFormAccessor = (attr: String, default: String, rawForm: Map[String, Seq[String]]) => { rawForm.getOrElse(attr, List(default)).head }
  val formAccessor = (attr: String, default: String, form: models.Form) => {
    val instanceMirror = runtimeMirror(getClass.getClassLoader).reflect(form)
    val temp = scala.reflect.runtime.universe.typeOf[models.Form].member(TermName(attr)) match {
      case m: MethodSymbol if m.isCaseAccessor => {
        instanceMirror.reflectMethod(m)() match {
          case value: String => { value }
          case Some(value) => { value.asInstanceOf[String] }
          case None => { default }
          case _ => { throw new Exception("Did not receive either an Option[String] or String accessing what should be a form parameter.") }
          }
        }
      case _ => { throw new Exception("The provided form attribute was not a case accessor; this is the wrong accessor method to use in this case.") }
    }
    temp
  }
  
  def populate_listid_accessor(accessors: Map[String, (String => String)])(implicit request: Request[Any]): Map[String, (String => String)] = {
    assert(accessors != null, "populate_listid_accessor must be given accessors to append to.")
    val referer = request.headers.get("Referer").getOrElse("")
    Logger.debug("referer: " + referer)
    if(referer.contains("/energy-savings")) {
      return accessors ++ Map(
          "listid" -> { (default: String) => "lg_solar_form" }
          )
    } else {
      return accessors ++ Map(
          "listid" -> { (default: String) => "solar_full_form" }
          )
    }
  }
  
  type AccessorMap = Map[String, (String => String)]
  def post_accessors(rawForm: Map[String, Seq[String]], form: EasierSolarForm)(implicit domain: String, request: Request[Any]): AccessorMap = {
    var accessors = Map.empty[String, (String => String)]
    accessors = populate_listid_accessor(accessors)(request)
    accessors ++ Map(
      "credit_range" -> { (default: String) => rawFormAccessor("form.credit_range", default, rawForm) },
      "ip" -> { (default: String) => rawFormAccessor("ip", default, rawForm) },
      "User-Agent" -> { (default: String) => request.headers.toMap.getOrElse("User-Agent", List(default)).mkString },
      "first_name" -> { (default: String) => formAccessor("first_name", default, form.form) },
      "last_name" -> { (default: String) => formAccessor("last_name", default, form.form) },
      "full_name" -> { (default: String) => rawFormAccessor("form.full_name", default, rawForm) },
      "email" -> { (default: String) => formAccessor("email", default, form.form) },
      "zip" -> { (default: String) => formAccessor("zip", default, form.form) },
      "city" -> { (default: String) => formAccessor("city", default, form.form) },
      "state" -> { (default: String) => formAccessor("state", default, form.form) },
      "street" -> { (default: String) => formAccessor("street", default, form.form) },
      "property_ownership" -> { (default: String) => formAccessor("property_ownership", default, form.form) },
      "electric_bill" -> { (default: String) => URLEncoder.encode(formAccessor("electric_bill", default, form.form).replaceAll("&", "&amp;"), "UTF-8") },
      "electric_company" -> { (default: String) => URLEncoder.encode(formAccessor("electric_company", default, form.form), "UTF-8") },
      "phone_home" -> { (default: String) => simplifyPhone(formAccessor("phone_home", default, form.form)) },
      "leadid_token" -> { (default: String) => formAccessor("leadid_token", default, form.form) },
      "xxTrustedFormToken" -> { (default: String) => rawFormAccessor("xxTrustedFormToken", default, rawForm) },
      "xxTrustedFormCertUrl" -> { (default: String) => rawFormAccessor("xxTrustedFormCertUrl", default, rawForm) },
//      "listid" -> { (default: String) => rawFormAccessor("listid", default, rawForm) },
      "domtok" -> { (default: String) => formAccessor("domtok", default, form.form) },
      "ref" -> { (default: String) => formAccessor("ref", default, form.form) },
      "robot_id" -> { (default: String) => rawFormAccessor("robot_id", default, rawForm) },
      "local_hour" -> { (default: String) => rawFormAccessor("local_hour", default, rawForm) },
      "asid" -> { (default: String) => domain},
      "dob" -> { (default: String) => rawFormAccessor("form.dob", default, rawForm) }
    )
  }
  
  def ping_accessors(rawForm: Map[String, Seq[String]])(implicit domain: String, request: Request[Any]): AccessorMap = {
    var accessors : AccessorMap = Map.empty[String, (String => String)];
    accessors = populate_listid_accessor(accessors);
    accessors ++ Map(
        "credit_range" -> { (default: String) => rawFormAccessor("form.credit_range", default, rawForm) },
        "leadid_token" -> { (default: String) => rawFormAccessor("form.leadid_token", default, rawForm) },
        //"listid" -> { (default: String) => rawFormAccessor("listid", default, rawForm) },
        "domtok" -> { (default: String) => rawFormAccessor("form.domtok", default, rawForm) },
        "ref" -> { (default: String) => rawFormAccessor("form.ref", default, rawForm) },
        "ip" -> { (default: String) => rawFormAccessor("ip", default, rawForm) },
        "User-Agent" -> { (default: String) => request.headers.toMap.getOrElse("User-Agent", List(default)).mkString },
        "first_name" -> { (default: String) => rawFormAccessor("form.first_name", default, rawForm) },
        "last_name" -> { (default: String) => rawFormAccessor("form.last_name", default, rawForm) },
        "full_name" -> { (default: String) => rawFormAccessor("form.full_name", default, rawForm) },
        "email" -> { (default: String) => rawFormAccessor("form.email", default, rawForm) },
        "zip" -> { (default: String) => rawFormAccessor("form.zip", default, rawForm) },
        "city" -> { (default: String) => rawFormAccessor("form.city", default, rawForm) },
        "state" -> { (default: String) => rawFormAccessor("form.state", default, rawForm) },
        "street" -> { (default: String) => rawFormAccessor("form.street", default, rawForm) },
        "property_ownership" -> { (default:String) => rawFormAccessor("form.property_ownership", default, rawForm) },
        "electric_bill" -> { (default: String) => URLEncoder.encode(rawFormAccessor("form.electric_bill", default, rawForm), "UTF-8") },
        "electric_company" -> { (default: String) => URLEncoder.encode(rawFormAccessor("form.electric_company", default, rawForm).replaceAll("&", "&amp;"), "UTF-8") },
        "phone_home" -> { (default: String) => simplifyPhone(rawFormAccessor("form.phone_home", default, rawForm)) },
        "xxTrustedFormToken" -> { (default: String) => rawFormAccessor("xxTrustedFormToken", default, rawForm) },
        "xxTrustedFormCertUrl" -> { (default: String) => rawFormAccessor("xxTrustedFormCertUrl", default, rawForm) },
        "robot_id" -> { (default: String) => rawFormAccessor("robot_id", default, rawForm) },
        "local_hour" -> { (default: String) => rawFormAccessor("local_hour", default, rawForm) },
        "asid" -> { (default: String) => domain },
        "dob" -> { (default: String) => rawFormAccessor("form.dob", default, rawForm) }
        );
  }
  
  def subscribeEmail = Action.async { implicit request =>
      val rawForm = request.body.asFormUrlEncoded.get
      val body = "email=%s&listid=%s&domtok=%s&ref=%s".format(
            rawForm.getOrElse("form.email", List("")).head,
            rawForm.getOrElse("form.listid", List("")).head,
            rawForm.getOrElse("form.domtok", List("")).head,
            rawForm.getOrElse("form.ref", List("")).head)
      Logger.debug(body)
      val url = configuration.getString("leadpost.base.uri").getOrElse(throw new Exception("Missing Leadpost Base URI")) + "/subscribe"
      ws.url(url)
      .withFollowRedirects(true)
      .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .withRequestTimeout(15 seconds)
      .post(body).map { response =>
          Status(response.status)(response.body)
      }    
  }
  
  def queryWhitepages(rawForm: Map[String, Seq[String]], easierSolarForm: EasierSolarForm) : Future[Option[WhitepagesResponse]] = {
    if(!easierSolarForm.form.first_name.getOrElse("").equalsIgnoreCase("mickey") || !easierSolarForm.form.last_name.getOrElse("").equalsIgnoreCase("mouse")) {
      val whitepagesRequest = WhitepagesRequest(
          null,
          easierSolarForm.form.first_name.getOrElse(""),
          easierSolarForm.form.last_name.getOrElse(""),
          easierSolarForm.form.phone_home.getOrElse(""),
          easierSolarForm.form.email,
          rawForm.getOrElse("ip", List("")).head,
          easierSolarForm.form.street.getOrElse(""),
          null,
          easierSolarForm.form.city.getOrElse(""),
          easierSolarForm.form.zip.getOrElse(""),
          easierSolarForm.form.state.getOrElse(""),
          null)

      whitepagesDAO.query(whitepagesRequest)
    } else {
      Future { None }
    }
  }
        
  def constructParametersFromWhitepages(whitepagesInfo: WhitepagesResponse): Map[String, Seq[String]] = {
    import controllers.LeadPathController._
    // Request them
    var parameterNames: Seq[String] = Seq()
    var parameterValues: Seq[Seq[String]] = Seq()
    
    if(!whitepagesInfo.error.isEmpty) {
      parameterNames = parameterNames ++ Seq(
          WHITEPAGES_ERROR_NAME,
          WHITEPAGES_ERROR_MESSAGE
          )
          
      parameterValues = parameterValues ++ Seq(
          whitepagesInfo.error.get.name,
          whitepagesInfo.error.get.message).map(Seq(_))
    }
    
    if(!whitepagesInfo.phoneCheck.isEmpty) {
      parameterNames = parameterNames ++ Seq(
          PHONE_CHECKS_ERROR,
          PHONE_CHECKS_WARNINGS,
          PHONE_CHECKS_IS_VALID,
          PHONE_CHECKS_PHONE_CONTACT_SCORE,
          PHONE_CHECKS_IS_CONNECTED,
          PHONE_CHECKS_PHONE_TO_NAME,
          PHONE_CHECKS_SUBSCRIBER_NAME,
          PHONE_CHECKS_SUBSCRIBER_AGE_RANGE,
          PHONE_CHECKS_SUBSCRIBER_GENDER,
          PHONE_CHECKS_COUNTRY_CODE,
          PHONE_CHECKS_IS_PREPAID,
          PHONE_CHECKS_LINE_TYPE,
          PHONE_CHECKS_CARRIER,
          PHONE_CHECKS_IS_COMMERCIAL)
          
      parameterValues = parameterValues ++ Seq(
          whitepagesInfo.phoneCheck.get.error.toString(),
          whitepagesInfo.phoneCheck.get.warnings.toString(),
          whitepagesInfo.phoneCheck.get.isValid.toString(),
          whitepagesInfo.phoneCheck.get.phoneContactScore.fold("")(_.toString()),
          whitepagesInfo.phoneCheck.get.isConnected.fold("")(_.toString()),
          whitepagesInfo.phoneCheck.get.phoneToName.fold("")(_.toString()),
          whitepagesInfo.phoneCheck.get.subscribeName.getOrElse(""),
          whitepagesInfo.phoneCheck.get.subscriberAgeRange.fold("")(_.toString()),
          whitepagesInfo.phoneCheck.get.subscriberGender.getOrElse(""),
          whitepagesInfo.phoneCheck.get.countryCode.getOrElse(""),
          whitepagesInfo.phoneCheck.get.isPrepaid.fold("")(_.toString()),
          whitepagesInfo.phoneCheck.get.lineType.fold("")(_.toString()),
          whitepagesInfo.phoneCheck.get.carrier.getOrElse(""),
          whitepagesInfo.phoneCheck.get.isCommercial.fold("")(_.toString())
          ).map(Seq(_))
      if(!whitepagesInfo.phoneCheck.get.subscriberAddress.isEmpty) {
        parameterNames = parameterNames ++ Seq(
            PHONE_CHECKS_SUBSCRIBER_ADDRESS_STREET_LINE_1,
            PHONE_CHECKS_SUBSCRIBER_ADDRESS_STREET_LINE_2,
            PHONE_CHECKS_SUBSCRIBER_ADDRESS_CITY,
            PHONE_CHECKS_SUBSCRIBER_ADDRESS_POSTAL_CODE,
            PHONE_CHECKS_SUBSCRIBER_ADDRESS_STATE_CODE,
            PHONE_CHECKS_SUBSCRIBER_ADDRESS_COUNTRY_CODE)

        
        parameterValues = parameterValues ++ Seq(
            whitepagesInfo.phoneCheck.get.subscriberAddress.get.streetLine1.getOrElse(""),
            whitepagesInfo.phoneCheck.get.subscriberAddress.get.streetLine2.getOrElse(""),
            whitepagesInfo.phoneCheck.get.subscriberAddress.get.city.getOrElse(""),
            whitepagesInfo.phoneCheck.get.subscriberAddress.get.postalCode.getOrElse(""),
            whitepagesInfo.phoneCheck.get.subscriberAddress.get.stateCode.getOrElse(""),
            whitepagesInfo.phoneCheck.get.subscriberAddress.get.countryCode.getOrElse("")
            ).map(Seq(_))

      }
    }
        
    if(!whitepagesInfo.addressCheck.isEmpty) {
      parameterNames = parameterNames ++ Seq(
          ADDRESS_CHECKS_ERROR,
          ADDRESS_CHECKS_WARNINGS,
          ADDRESS_CHECKS_IS_VALID,
          ADDRESS_CHECKS_DIAGNOSTICS,
          ADDRESS_CHECKS_ADDRESS_CONTACT_SCORE,
          ADDRESS_CHECKS_IS_ACTIVE,
          ADDRESS_CHECKS_ADDRESS_TO_NAME,
          ADDRESS_CHECKS_RESIDENT_NAME,
          ADDRESS_CHECKS_RESIDENT_AGE_RANGE,
          ADDRESS_CHECKS_RESIDENT_GENDER,
          ADDRESS_CHECKS_TYPE,
          ADDRESS_CHECKS_IS_COMMERCIAL,
          ADDRESS_CHECKS_RESIDENT_PHONE)
          
      parameterValues = parameterValues ++ Seq(
          whitepagesInfo.addressCheck.get.error.toString(),
          whitepagesInfo.addressCheck.get.warnings.toString(),
          whitepagesInfo.addressCheck.get.isValid.fold("")(_.toString()),
          whitepagesInfo.addressCheck.get.diagnostics.toString(),
          whitepagesInfo.addressCheck.get.addressContactScore.fold("")(_.toString()),
          whitepagesInfo.addressCheck.get.isActive.fold("")(_.toString()),
          whitepagesInfo.addressCheck.get.addressToName.fold("")(_.toString()),
          whitepagesInfo.addressCheck.get.residentName.getOrElse(""),
          whitepagesInfo.addressCheck.get.residentAgeRange.toString(),
          whitepagesInfo.addressCheck.get.residentGender.getOrElse(""),
          whitepagesInfo.addressCheck.get.addressType.toString(),
          whitepagesInfo.addressCheck.get.isCommercial.fold("")(_.toString()),
          whitepagesInfo.addressCheck.get.residentPhone.fold("")(_.toString())
          ).map(Seq(_))
    }
        
    if(!whitepagesInfo.emailCheck.isEmpty) {
      parameterNames = parameterNames ++ Seq(
          EMAIL_ADDRESS_CHECKS_ERROR,
          EMAIL_ADDRESS_CHECKS_WARNINGS,
          EMAIL_ADDRESS_CHECKS_IS_VALID,
          EMAIL_ADDRESS_CHECKS_DIAGNOSTICS,
          EMAIL_ADDRESS_CHECKS_EMAIL_CONTACT_SCORE,
          EMAIL_ADDRESS_CHECKS_IS_DISPOSABLE,
          EMAIL_ADDRESS_CHECKS_EMAIL_TO_NAME,
          EMAIL_ADDRESS_CHECKS_REGISTERED_NAME)
          
      parameterValues = parameterValues ++ Seq(
          whitepagesInfo.emailCheck.get.error.toString(),
          whitepagesInfo.emailCheck.get.warnings.toString(),
          whitepagesInfo.emailCheck.get.isValid.fold("")(_.toString()),
          whitepagesInfo.emailCheck.get.diagnostics.toString(),
          whitepagesInfo.emailCheck.get.emailContactScore.fold("")(_.toString()),
          whitepagesInfo.emailCheck.get.isDisposable.fold("")(_.toString()),
          whitepagesInfo.emailCheck.get.emailToName.fold("")(_.toString()),
          whitepagesInfo.emailCheck.get.registeredName.getOrElse("")
          ).map(Seq(_))
    }
        
    if(!whitepagesInfo.ipCheck.isEmpty) {
      parameterNames = parameterNames ++ Seq(
          IP_ADDRESS_CHECKS_ERROR,
          IP_ADDRESS_CHECKS_WARNINGS,
          IP_ADDRESS_CHECKS_IS_VALID,
          IP_ADDRESS_CHECKS_IS_PROXY,
          IP_ADDRESS_CHECKS_DISTANCE_FROM_ADDRESS,
          IP_ADDRESS_CHECKS_DISTANCE_FROM_PHONE,
          IP_ADDRESS_CHECKS_CONNECTION_TYPE)
          
      parameterValues = parameterValues ++ Seq(
          whitepagesInfo.ipCheck.get.error.toString(),
          whitepagesInfo.ipCheck.get.warnings.toString(),
          whitepagesInfo.ipCheck.get.isValid.fold("")(_.toString()),
          whitepagesInfo.ipCheck.get.isProxy.fold("")(_.toString()),
          whitepagesInfo.ipCheck.get.distanceFromAddress.fold("")(_.toString()),
          whitepagesInfo.ipCheck.get.distanceFromPhone.fold("")(_.toString()),
          whitepagesInfo.ipCheck.get.connectionType.fold("")(_.toString())
          ).map(Seq(_))
          
      if(!whitepagesInfo.ipCheck.get.geolocation.isEmpty) {
          parameterNames = parameterNames ++ Seq(
              IP_ADDRESS_CHECKS_GEOLOCATION_POSTAL_CODE,
              IP_ADDRESS_CHECKS_GEOLOCATION_CITY_NAME,
              IP_ADDRESS_CHECKS_GEOLOCATION_SUBDIVISION,
              IP_ADDRESS_CHECKS_GEOLOCATION_COUNTRY_NAME,
              IP_ADDRESS_CHECKS_GEOLOCATION_COUNTRY_CODE,
              IP_ADDRESS_CHECKS_GEOLOCATION_CONTINENT_CODE)

          val geolocation = whitepagesInfo.ipCheck.get.geolocation.get;
          parameterValues = parameterValues ++ Seq(
              geolocation.postalCode.getOrElse(""),
              geolocation.cityName.getOrElse(""),
              geolocation.subdivision.getOrElse(""),
              geolocation.countryName.getOrElse(""),
              geolocation.countryCode.getOrElse(""),
              geolocation.continentCode.getOrElse("")).map(Seq(_))
      }
    }
    
    (parameterNames zip parameterValues).toMap
  }
}

object LeadPathController {
  val WHITEPAGES_ERROR_NAME = "whitepages.error.name";
  val WHITEPAGES_ERROR_MESSAGE = "whitepages.error.message";
  val PHONE_CHECKS_ERROR = "phone_checks.error"
  val PHONE_CHECKS_WARNINGS = "phone_checks.warnings"
  val PHONE_CHECKS_IS_VALID = "phone_checks.is_valid"
  val PHONE_CHECKS_PHONE_CONTACT_SCORE = "phone_checks.phone_contact_score"
  val PHONE_CHECKS_IS_CONNECTED = "phone_checks.is_connected"
  val PHONE_CHECKS_PHONE_TO_NAME = "phone_checks.phone_to_name"
  val PHONE_CHECKS_SUBSCRIBER_NAME = "phone_checks.subscriber_name"
  val PHONE_CHECKS_SUBSCRIBER_AGE_RANGE = "phone_checks.subscriber_age_range"
  val PHONE_CHECKS_SUBSCRIBER_GENDER = "phone_checks.subscriber_gender"
  val PHONE_CHECKS_SUBSCRIBER_ADDRESS_STREET_LINE_1 = "phone_checks.subscriber_address.street_line_1"
  val PHONE_CHECKS_SUBSCRIBER_ADDRESS_STREET_LINE_2 = "phone_checks.subscriber_address.street_line_2"
  val PHONE_CHECKS_SUBSCRIBER_ADDRESS_CITY = "phone_checks.subscriber_address.city"
  val PHONE_CHECKS_SUBSCRIBER_ADDRESS_POSTAL_CODE = "phone_checks.subscriber_address.postal_code"
  val PHONE_CHECKS_SUBSCRIBER_ADDRESS_STATE_CODE = "phone_checks.subscriber_address.state_code"
  val PHONE_CHECKS_SUBSCRIBER_ADDRESS_COUNTRY_CODE = "phone_checks.subscriber_address.country_code"
  val PHONE_CHECKS_COUNTRY_CODE = "phone_checks.country_code"
  val PHONE_CHECKS_IS_PREPAID = "phone_checks.is_prepaid"
  val PHONE_CHECKS_LINE_TYPE ="phone_checks.line_type"
  val PHONE_CHECKS_CARRIER = "phone_checks.carrier"
  val PHONE_CHECKS_IS_COMMERCIAL ="phone_checks.is_commercial"
  val ADDRESS_CHECKS_ERROR = "address_checks.error"
  val ADDRESS_CHECKS_WARNINGS = "address_checks.warnings"
  val ADDRESS_CHECKS_IS_VALID = "address_checks.is_valid"
  val ADDRESS_CHECKS_DIAGNOSTICS = "address_checks.diagnostics"
  val ADDRESS_CHECKS_ADDRESS_CONTACT_SCORE = "address_checks.address_contact_score"
  val ADDRESS_CHECKS_IS_ACTIVE = "address_checks.is_active"
  val ADDRESS_CHECKS_ADDRESS_TO_NAME = "address_checks.address_to_name"
  val ADDRESS_CHECKS_RESIDENT_NAME = "address_checks.resident_name"
  val ADDRESS_CHECKS_RESIDENT_AGE_RANGE = "address_checks.resident_age_range"
  val ADDRESS_CHECKS_RESIDENT_GENDER = "address_checks.resident_gender"
  val ADDRESS_CHECKS_TYPE = "address_checks.type"
  val ADDRESS_CHECKS_IS_COMMERCIAL = "address_checks.is_commercial"
  val ADDRESS_CHECKS_RESIDENT_PHONE = "address_checks.resident_phone"
  val EMAIL_ADDRESS_CHECKS_ERROR = "email_address_checks.error"
  val EMAIL_ADDRESS_CHECKS_WARNINGS = "email_address_checks.warnings"
  val EMAIL_ADDRESS_CHECKS_IS_VALID = "email_address_checks.is_valid"
  val EMAIL_ADDRESS_CHECKS_DIAGNOSTICS = "email_address_checks.diagnostics"
  val EMAIL_ADDRESS_CHECKS_EMAIL_CONTACT_SCORE = "email_address_checks.email_contact_score"
  val EMAIL_ADDRESS_CHECKS_IS_DISPOSABLE = "email_address_checks.is_disposable"
  val EMAIL_ADDRESS_CHECKS_EMAIL_TO_NAME = "email_address_checks.email_to_name"
  val EMAIL_ADDRESS_CHECKS_REGISTERED_NAME = "email_address_checks.registered_name"
  val IP_ADDRESS_CHECKS_ERROR = "ip_address_checks.error"
  val IP_ADDRESS_CHECKS_WARNINGS = "ip_address_checks.warnings"
  val IP_ADDRESS_CHECKS_IS_VALID = "ip_address_checks.is_valid"
  val IP_ADDRESS_CHECKS_IS_PROXY = "ip_address_checks.is_proxy"
  val IP_ADDRESS_CHECKS_GEOLOCATION_POSTAL_CODE = "ip_address_checks.geolocation.postal_code"
  val IP_ADDRESS_CHECKS_GEOLOCATION_CITY_NAME = "ip_address_checks.geolocation.city_name"
  val IP_ADDRESS_CHECKS_GEOLOCATION_SUBDIVISION = "ip_address_checks.geolocation.subdivision"
  val IP_ADDRESS_CHECKS_GEOLOCATION_COUNTRY_NAME = "ip_address_checks.geolocation.country_name"
  val IP_ADDRESS_CHECKS_GEOLOCATION_COUNTRY_CODE = "ip_address_checks.geolocation.country_code"
  val IP_ADDRESS_CHECKS_GEOLOCATION_CONTINENT_CODE = "ip_address_checks.geolocation.continent_code"
  val IP_ADDRESS_CHECKS_DISTANCE_FROM_ADDRESS = "ip_address_checks.distance_from_address"
  val IP_ADDRESS_CHECKS_DISTANCE_FROM_PHONE = "ip_address_checks.distance_from_phone"
  val IP_ADDRESS_CHECKS_CONNECTION_TYPE = "ip_address_checks.connection_type"
  val LEAD_CACHE_DURATION_KEY = "lynx.lead_cache_duration"

  var ATTRIBUTES = Map[String, Long]()

  def loadValues()(implicit attributeDAO: AttributeDAO) = {

    if (!ATTRIBUTES.nonEmpty) {      
      attributeDAO.all().map { db_attributes =>
        db_attributes.map { attribute =>
          Logger.debug("Loading attribute: " + attribute.name.toLowerCase)
          ATTRIBUTES += (attribute.name.toLowerCase -> attribute.id.get)
        }
      }
    }
  }
  
  val UTM_CAMPAIGN = "utm_campaign"
  val ADGROUPID = "adgroupid"
  val UTM_SOURCE = "utm_source"
  val UTM_MEDIUM = "utm_medium"
  
  private def constructParametersFromAttributes(sid: Long)(implicit sessionAttributeDAO: SessionAttributeDAO): Future[Map[String, Seq[String]]] = {
    assert(sessionAttributeDAO != null, "Attribute DAO must be provided to constructParametersFromAttributes")
    
    val attributeIds = scala.collection.mutable.ArrayBuffer.empty[Long]
    var attributeId : Long = 0
    val parameterNames = scala.collection.mutable.ArrayBuffer.empty[String]
    ATTRIBUTES.get(UTM_CAMPAIGN).fold()(id => { attributeIds += id })
    ATTRIBUTES.get(ADGROUPID).fold()(id => { attributeIds += id })
    ATTRIBUTES.get(UTM_SOURCE).fold()(id => { attributeIds += id })
    ATTRIBUTES.get(UTM_MEDIUM).fold()(id => { attributeIds += id })
    
    for {
      attributeValuesRaw <- sessionAttributeDAO.findBySessionIdAndAttributes(sid, attributeIds.toList)
      attributeValues <- {
        val attributesSorted = ListBuffer.empty[String]
        for(attribute <- attributeValuesRaw) {
          if(attribute.attribute_id.equals(ATTRIBUTES.get(UTM_CAMPAIGN))) {
             parameterNames += UTM_CAMPAIGN
             attributesSorted += attribute.value.getOrElse("")
          } else if(attribute.attribute_id.equals(ATTRIBUTES.get(ADGROUPID))) {
            parameterNames += ADGROUPID
            attributesSorted += attribute.value.getOrElse("")
          } else if(attribute.attribute_id.equals(ATTRIBUTES.get(UTM_SOURCE))) {
            parameterNames += UTM_SOURCE
            attributesSorted += attribute.value.getOrElse("")
          } else if(attribute.attribute_id.equals(ATTRIBUTES.get(UTM_MEDIUM))) {
            parameterNames += UTM_MEDIUM
            attributesSorted += attribute.value.getOrElse("")
          }
        }
        Future.successful(attributesSorted.map( x => Seq(x) ).seq)
      }
      parameters <- Future.successful((parameterNames zip attributeValues).toMap)
    } yield parameters
  }

  def createWebsocketActor(leadId: Option[String] = None, leadPathArrival: Option[LeadpathArrival] = None)
                          (implicit system: ActorSystem, configuration: play.api.Configuration): (Option[ActorSelection], Boolean) = {

    def createSocket(leadId: String): (ActorSelection, Boolean) = {
      val path = s"/user/${LeadInfoActor.PREFIX}${leadId}";
      Logger.info(s"Creating actor in path ${path}")
      try {
        val newActor = system.actorOf(LeadInfoActor.props(configuration.getInt(LeadPathController.LEAD_CACHE_DURATION_KEY).getOrElse(LeadInfoActor.SHUTDOWN_DURATION_DEFAULT)), LeadInfoActor.PREFIX + leadId)
        // Not publishing the lead to the actor here because there could be a race condition with the call to post_disposition. Consider putting a timestamp on it?
        Logger.info("Path of created actor: " + newActor.path.toStringWithoutAddress)

        return (system.actorSelection(path), true);
      } catch {
        case e: InvalidActorNameException => {
          Logger.info("Actor already exists; will not re-create and will send keep-alive")
          val actor = system.actorSelection(path)
          actor ! "Keep alive"
          return (actor, false);
        }
      }
    }

    (leadId, leadPathArrival) match {
      case (Some(leadId), _) =>
        val(actor, isNew) = createSocket(leadId)
        return (Some(actor), isNew)
      case (_, Some(leadpathArrival)) =>
        val(actor, isNew) = createSocket(leadpathArrival.lead.id)
        return (Some(actor), isNew)
      case _ =>
        Logger.error("No socket created, no lead id provided")
        return (None, false)
    }
  }
}
