package controllers;

import org.specs2.mutable.Specification;
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import dao.WhitepagesErrorMessage
import dao.WhitepagesResponse
import play.Logger
import dao.PhoneCheckResponse
import dao.WhitepagesAddressResponse
import controllers.LeadPathController._
import dao.EmailCheckResponse
import dao.IpCheckResponse
import dao.Geolocation
import utils.utilities.EasierSolarForm
import org.specs2.mock._
import play.api.libs.ws.WSClient
import dao.WhitepagesDAO
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import play.api.http.Writeable
import scala.concurrent.duration.Duration
import scala.reflect._
import scala.concurrent.Future
import scala.concurrent.Await
import dao.WhitepagesRequest
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Headers
import models.Form

class LeadPathControllerTest extends Specification with Mockito {
	
	val config: Config = ConfigFactory.load("dev.conf")
	val configuration: Configuration = new Configuration(config)
  val app = new GuiceApplicationBuilder()
	    .loadConfig(configuration)
			.build

	"With a whitepages response that only has an error." in {
	  val testErrorMessage = "Test Error Message"
	  val testErrorName = "Test Error"
	  val response = WhitepagesResponse(
	      Some(WhitepagesErrorMessage(
	          testErrorMessage,
	          testErrorName
	          )),
	      None,
	      None,
	      None,
	      None
	      )
	  val controller = new LeadPathController(
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null,
	      null)
	  val parameters = controller.constructParametersFromWhitepages(response)
	  "Error message" should {
	    { "have the correct name" in { parameters.get("whitepages.error.name") must beSome(List(testErrorName)) } }
	    { "have the correct message" in { parameters.get("whitepages.error.message") must beSome(List(testErrorMessage)) } }
	  }
	}
	
  "With a whitepages response that only a phone check." in {
    val testName = "Test Name"
    val testStreet1 = "Test street 1"
    val testCity = "Test city"
    val testPostalCode = "Test postal code"
    val testStateCode = "MA"
    val testCountryCode = "US"
	  val response = WhitepagesResponse(
	      None,
	      Some(PhoneCheckResponse(
	          None,
	          None,
	          Some(true),
	          Some(10),
	          Some(true),
	          Some("No match"),
	          Some(testName),
	          Some("35-39"),
	          None,
	          Some(WhitepagesAddressResponse(
	              Some(testStreet1),
	              None,
	              Some(testCity),
	              Some(testPostalCode),
	              Some(testStateCode),
	              Some(testCountryCode))),
	          Some(testCountryCode),
	          Some("Land line"),
	          Some("Test Carrier"),
	          Some(false),
	          None
	          )),
	      None,
	      None,
	      None
	      )
	  val controller = new LeadPathController(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
	  val parameters = controller.constructParametersFromWhitepages(response)
	  Logger.error(parameters.toString())
	  "Phone check message" should {
	    { "have the correct error" in { parameters.get(PHONE_CHECKS_ERROR) must beSome(List("None")) } }
	    { "have the correct warnings" in { parameters.get(PHONE_CHECKS_WARNINGS) must beSome(List("None")) } }
	    { "have the correct is_valid" in { parameters.get(PHONE_CHECKS_IS_VALID) must beSome(List("true")) } }
	    { "have the correct phone_contact_score" in { parameters.get(PHONE_CHECKS_PHONE_CONTACT_SCORE) must beSome(List("10")) } }
	    { "have the correct is_connected" in { parameters.get(PHONE_CHECKS_IS_CONNECTED) must beSome(List("true")) } }
	    { "have the correct phone_to_match" in { parameters.get(PHONE_CHECKS_PHONE_TO_NAME) must beSome(List("No match")) } }
	    { "have the correct subscriber_name" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_NAME) must beSome(List(testName) ) } }
	    { "have the correct subscriber age range" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_AGE_RANGE) must beSome(List("35-39")) } }
	    { "have the correct gender" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_GENDER) must beSome(List("")) } }
	    { "have the correct street 1" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_ADDRESS_STREET_LINE_1) must beSome(List(testStreet1)) } }
	    { "have the correct street 2" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_ADDRESS_STREET_LINE_2) must beSome(List("")) } }
	    { "have the correct city" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_ADDRESS_CITY) must beSome(List(testCity)) } }
	    { "have the correct postal code" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_ADDRESS_POSTAL_CODE) must beSome(List(testPostalCode)) } }
	    { "have the correct state code" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_ADDRESS_STATE_CODE) must beSome(List(testStateCode)) } }
	    { "have the correct country code" in { parameters.get(PHONE_CHECKS_SUBSCRIBER_ADDRESS_COUNTRY_CODE) must beSome(List(testCountryCode)) } }
	  }
	}

  "With a whitepages response that only an email check." in {
    val testResidentName = "Test Name"
    val response = WhitepagesResponse(
	      None,
	      None,
	      None,
	      Some(EmailCheckResponse(
	          None,
	          None,
	          None,
	          Some(Seq("Domain does not support validation (accepts all mailboxes)", "Syntax OK, domain exists, and mailbox does not reject mail")),
	          Some(4),
	          Some(true),
	          Option("No name found"),
	          Some(testResidentName))),
	      None
	      )
	  val controller = new LeadPathController(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
	  val parameters = controller.constructParametersFromWhitepages(response)
	  Logger.error(parameters.toString())
	  "Phone check message" should {
	    { "have the correct error" in { parameters.get(EMAIL_ADDRESS_CHECKS_ERROR) must beSome(List("None")) } }
	    { "have the correct warnings" in { parameters.get(EMAIL_ADDRESS_CHECKS_WARNINGS) must beSome(List("None")) } }
	    { "have the correct is_valid" in { parameters.get(EMAIL_ADDRESS_CHECKS_IS_VALID) must beSome(List("")) } }
	    { "have the correct email diagnostics" in { parameters.get(EMAIL_ADDRESS_CHECKS_DIAGNOSTICS) must beSome(List("List(Domain does not support validation (accepts all mailboxes), Syntax OK, domain exists, and mailbox does not reject mail)")) } }
	    { "have the correct email contact score" in { parameters.get(EMAIL_ADDRESS_CHECKS_EMAIL_CONTACT_SCORE) must beSome(List("4")) } }
	    { "have the correct is disposable" in { parameters.get(EMAIL_ADDRESS_CHECKS_IS_DISPOSABLE) must beSome(List("true")) } }
	    { "have the correct email to name" in { parameters.get(EMAIL_ADDRESS_CHECKS_EMAIL_TO_NAME) must beSome(List("No name found") ) } }
	    { "have the correct regsitered name" in { parameters.get(EMAIL_ADDRESS_CHECKS_REGISTERED_NAME) must beSome(List(testResidentName)) } }
	  }
	}
  
  "With a whitepages response that only an ip check." in {
    val testPostalCode = "test postal code"
    val testCountryName = "test country name"
    val testContinentCode = "test continent name"
    val testCountryCode = "test country code"
    val response = WhitepagesResponse(
	      None,
	      None,
	      None,
	      None,
	      Some(IpCheckResponse(
	          None,
	          None,
	          Some(true),
	          Some(true),
	          Some(Geolocation(
	              Some(testPostalCode),
	              None,
	              None,
	              Some(testCountryName),
	              Some(testContinentCode),
	              Some(testCountryCode)
	              )),
	          Some(1),
	          Some(2),
	          None))
	      )
	  val controller = new LeadPathController(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
	  val parameters = controller.constructParametersFromWhitepages(response)
	  Logger.error(parameters.toString())
	  "Phone check message" should {
	    { "have the correct error" in { parameters.get(IP_ADDRESS_CHECKS_ERROR) must beSome(List("None")) } }
	    { "have the correct warnings" in { parameters.get(IP_ADDRESS_CHECKS_WARNINGS) must beSome(List("None")) } }
	    { "have the correct is_valid" in { parameters.get(IP_ADDRESS_CHECKS_IS_VALID) must beSome(List("true")) } }
	    { "have the correct is proxy" in { parameters.get(IP_ADDRESS_CHECKS_IS_PROXY) must beSome(List("true")) } }
	    { "have the correct postal code" in { parameters.get(IP_ADDRESS_CHECKS_GEOLOCATION_POSTAL_CODE) must beSome(List(testPostalCode)) } }
	    { "have the correct city name" in { parameters.get(IP_ADDRESS_CHECKS_GEOLOCATION_CITY_NAME) must beSome(List("")) } }
	    { "have the correct subdivision" in { parameters.get(IP_ADDRESS_CHECKS_GEOLOCATION_SUBDIVISION) must beSome(List("")) } }
	    { "have the correct country name" in { parameters.get(IP_ADDRESS_CHECKS_GEOLOCATION_COUNTRY_NAME) must beSome(List(testCountryName)) } }
	    { "have the correct continent code" in { parameters.get(IP_ADDRESS_CHECKS_GEOLOCATION_CONTINENT_CODE) must beSome(List(testContinentCode)) } }
	    { "have the correct country code" in { parameters.get(IP_ADDRESS_CHECKS_GEOLOCATION_COUNTRY_CODE) must beSome(List(testCountryCode)) } }
	    { "have the correct distance from address" in { parameters.get(IP_ADDRESS_CHECKS_DISTANCE_FROM_ADDRESS) must beSome(List("1")) } }
	    { "have the correct distance from phone" in { parameters.get(IP_ADDRESS_CHECKS_DISTANCE_FROM_PHONE) must beSome(List("2") ) } }
	    { "have the correct connection type" in { parameters.get(IP_ADDRESS_CHECKS_CONNECTION_TYPE) must beSome(List("")) } }
	  }
	}
  
  "With a valid form and request, posting to Leadpath" in {
    val config: Config = ConfigFactory.load("dev.conf")
    val configuration: Configuration = new Configuration(config)
    
    // Initialize form
    val easierSolarForm = EasierSolarForm.apply(null, Form(
        None,
        None,
        None,
        "first name",
        "last name",
        "email",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None))
    // Initialize rawForm
    val rawForm = Map.empty[String, Seq[String]]
    // Initialize sid
    val sid = 0;
    val mockClient = mock[WSClient]
    val mockRequest = mock[WSRequest]
    val mockResponse = mock[WSResponse]
    val mockIncomingRequest = mock[Request[AnyContent]]
    val mockHeaders = mock[Headers]
    mockHeaders.toMap returns Map.empty[String, Seq[String]]
    mockIncomingRequest.headers returns mockHeaders
    mockClient.url(anyString) returns mockRequest
    mockRequest.withFollowRedirects(anyBoolean) returns mockRequest
    mockRequest.withHeaders("Content-Type" -> "application/x-www-form-urlencoded") returns mockRequest
    mockRequest.withRequestTimeout(any(classTag[Duration])) returns mockRequest
    mockRequest.post(any(classTag[Map[String, Seq[String]]]))(any(classTag[Writeable[Map[String, Seq[String]]]])) returns Future.successful(mockResponse)
    
    val mockWpDAO = mock[WhitepagesDAO]
    mockWpDAO.query(any(classTag[WhitepagesRequest]))(any(classTag[WSClient])) returns Future.successful(None)
    val controller = new LeadPathController(null, null, null, null, null, null, null, null, mockClient, null, null, null, null, null, null, mockWpDAO, null, configuration)
    // TODO: With a None coming back from the WhitepagesDAO, the WSClient should still be called with the form parameters.
    val responseFuture = controller.postDataToLeadpath(easierSolarForm, rawForm, sid)(mockIncomingRequest, "domain")
    
    // Have to wait for a result, even if I don't care, so any errors in execution can be thrown.
    Await.result(responseFuture, Duration.Inf)
    "with a None coming from WhitepagesDAO" should {
      // TODO: Verify that mockRequest was called
      "have called the WhitepagesDAO" in { there was one(mockWpDAO).query(any[WhitepagesRequest])(any(classTag[WSClient])) }
      
      "still have called the WSClient" in { there was one(mockClient).url(any[String]) }
    }
  }
}
