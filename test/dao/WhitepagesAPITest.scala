package dao

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import dao.whitepages._
import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError
import play.Logger
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSClient
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class WhitepagesAPITest extends Specification {
  
  val config: Config = ConfigFactory.load("dev.conf")
  val configuration: Configuration = new Configuration(config)
  val app = new GuiceApplicationBuilder()
      .loadConfig(configuration)
      .build
      
  "The DAO" in {
    val request = WhitepagesRequest(
        null,
        "Jonathan",
        "Card",
        "3039163966",
        "joncard93@hotmail.com",
        "11.11.11.11",
        "69 Merriam St",
        "Apt 3",
        "Somerville",
        "02143",
        "MA",
        "US"
        )
    val injector = app.injector
    implicit val ws: WSClient = injector.instanceOf[WSClient]
    val dao = injector.instanceOf[WhitepagesDAO]
    val responseFuture = dao.query(request)
    val response = Await.result(responseFuture, Duration.Inf)
    Logger.error(response.toString())
    failure("Not implemented")
  }
  
  "The phone check value" in {
    val testErrorMessage = "Test Error"
    val testErrorName = "TestError"
    val testWarning = "One warning"
    val testSubscriberName = "Drama Number"
    val testStreet1 = "302 Gorham Ave"
    val testValue = s"""{
      "error": {"message": "${testErrorMessage}", "name": "${testErrorName}"},
      "warnings": ["${testWarning}"],
      "is_valid": true,
      "phone_contact_score": 2,
      "is_connected": null,
      "phone_to_name": "Match",
      "subscriber_name": "${testSubscriberName}",
      "subscriber_age_range": "30-34",
      "subscriber_gender": "Male",
      "subscriber_address": {
        "street_line_1": "${testStreet1}",
        "street_line_2": null,
        "city": "Ashland",
        "postal_code": "59004",
        "state_code": "MT",
        "country_code": "US"
        },
      "country_code": "US",
      "line_type": "Non-fixed VOIP",
      "carrier": "Whatever",
      "is_prepaid": true,
      "is_commercial": false
      }"""
    
    val testObject: Option[PhoneCheckResponse] = Json.parse(testValue).validate[PhoneCheckResponse].fold(
        errors => {
          Logger.error("Errors during validation.")
          errors.foreach { errorTuple: (JsPath, Seq[ValidationError]) => Logger.error(s"${errorTuple._1.toString()}: ${errorTuple._2.toString()}") }
          None
        },
        x => Option(x)
        )
    testObject.isEmpty must beFalse.setMessage("Phone JSON did not validate correctly")
    val response = testObject.get
    "defined above" should {
      { "have a valid error" in {
        response.error.isEmpty must beFalse.setMessage("that is not empty.")
        "with a message that " should {
          { "have the correct message" in { response.error.get.message must beEqualTo(testErrorMessage) } }
          { "have the correct name" in { response.error.get.name must beEqualTo(testErrorName) } }
        } }
      }
      { "have warnings" in {
        "that" should {
          { "not be null" in { response.warnings.isEmpty must beFalse } }
          { "have the correct value" in {response.warnings.get.head must beEqualTo(testWarning) } }
        } }
      }
      { "have a true value for is_valid" in { response.isValid must beSome(true)} }
      { "have a 2 value for phone_contact_score" in { response.phoneContactScore must beSome(2) } }
      { "have a null value for is_connected" in { response.isConnected.isEmpty } }
      { "have a parsed value for phone_to_name" in { response.phoneToName must beSome("Match") } }
      { "have a subscriber_name that" in { response.subscribeName must beSome(testSubscriberName) } }
      { "have a subscriber_age_range" in { response.subscriberAgeRange must beSome("30-34") } }
      { "have a subscriber address that" should {
        // TODO: Check for the address's existence
        { "have a valid street" in { 
          "that" should {
            "exist" in { response.subscriberAddress.isEmpty must beFalse }
            "have the correct value" in { response.subscriberAddress.get.streetLine1.get must beEqualTo(testStreet1) } }
          }
        { "have a null street 2" in { response.subscriberAddress.get.streetLine2 must beNone } }
      } } }
      { "have a parsed value for line type" in { response.lineType must beSome("Non-fixed VOIP") } }
      { "have a true value for is_prepaid" in { response.isPrepaid must beSome(true) } }
      { "have a false value for is_commercial" in { response.isCommercial must beSome(false) } }
    }
  }
  
  "The address check value" in {
    val testResidentName = "Test Resident Name"
    val testValue = s"""{
      "error": null,
      "warnings": [],
      "is_valid": null,
      "diagnostics": ["Validated"],
      "address_contact_score": 2,
      "is_active": null,
      "address_to_name": "No name found",
      "resident_name": "${testResidentName}",
      "resident_age_range": "65-200",
      "resident_gender": null,
      "type": "Unknown address type",
      "is_commercial": false,
      "resident_phone": "No phone found"
      }"""
    val testObject: Option[AddressCheckResponse] = Json.parse(testValue).validate[AddressCheckResponse].fold(
        errors => {
          errors.foreach { errorTuple: (JsPath, Seq[ValidationError]) => Logger.debug(s"${errorTuple._1.toString()}: ${errorTuple._2.toString()}") }
          None
        },
        x => Option(x)
        )
    testObject.isEmpty must beFalse.setMessage("Address JSON did not validate correctly")
    val response = testObject.get
    "defined above" should {
      { "have no error object" in { response.error.isEmpty must beTrue } }
      { "have no warnings" in {
        response.warnings.isEmpty must beFalse.setMessage("that exists")
        response.warnings.get.size must beEqualTo(0).setMessage("with a length of 0")
      } }
      { "have a null is_valid" in { response.isValid.isEmpty must beTrue } }
      { "have a diagnostics array" in {
        response.diagnostics.isEmpty must beFalse.setMessage("that is not empty")
        response.diagnostics.head.isEmpty must beFalse setMessage("exist")
        response.diagnostics.head.size must beEqualTo(1).setMessage("have one element")
        "with the value Validated" in { response.diagnostics.get.head must beEqualTo("Validated") } }
      }
      { "have a null is_active" in { response.isActive.isEmpty must beTrue } }
      { "have an address_to_name" in { response.addressToName must beSome("No name found") } }
      { "have an age range of 65-200" in { response.residentAgeRange must beSome("65-200") } }
      { "have an unknown address type" in { response.addressType must beSome("Unknown address type") } }
    }
  }
  
  "The email check value" in {
    val testWarning1 = "One warning"
    val testWarning2 = "Two warning"
    val testSubscriberName = "Drama Number"
    val testStreet1 = "302 Gorham Ave"
    val testValue = s"""{
      "error": null,
      "warnings": ["${testWarning1}", "${testWarning2}"],
      "is_valid": true,
      "diagnostics": [
        "Domain does not support validation (accepts all mailboxes)",
        "Syntax OK, domain exists, and mailbox does not reject mail"
      ],
      "email_contact_score": 4,
      "is_disposable": false,
      "email_to_name": "No match",
      "registered_name": "No name found"
    }"""
    
    val testObject: Option[EmailCheckResponse] = Json.parse(testValue).validate[EmailCheckResponse].fold(
        errors => {
          Logger.error("Errors during validation.")
          errors.foreach { errorTuple: (JsPath, Seq[ValidationError]) => Logger.error(s"${errorTuple._1.toString()}: ${errorTuple._2.toString()}") }
          None
        },
        x => Option(x)
        )
    testObject.isEmpty must beFalse.setMessage("Email JSON did not validate correctly")
    val response = testObject.get
    "defined above" should {
      { "have no error object" in { response.error.isEmpty must beTrue } }
      { "have warnings" in {
        "that" should {
          { "not be null" in { response.warnings.isEmpty must beFalse } }
          { "have the correct first value" in { response.warnings.get.head must beEqualTo(testWarning1) } }
          { "have the correct second value" in { response.warnings.get.tail.head must beEqualTo(testWarning2) } }
        } }
      }
      { "have diagnostics" in {
        "that" should {
          { "not be null" in { response.diagnostics.isEmpty must beFalse } }
          { "exist" in { response.diagnostics.head.isEmpty must beFalse }}
          { "have the correct first value" in { response.diagnostics.get.head must beEqualTo("Domain does not support validation (accepts all mailboxes)") } }
          { "have the correct second value" in { response.diagnostics.get.tail.head must beEqualTo("Syntax OK, domain exists, and mailbox does not reject mail") } }
        } }
      }
      { "have an email_to_name" in { response.emailToName must beSome("No match") } }
    }
  }
  
  "The ip check value" in {
    val testPostalCode = "02145"
    val testCityName = "Somerville"
    val testCountryName = "United States"
    val testContinentCode = "NA"
    val testCountryCode = "US"
    val testValue = s"""{
      "error": null,
      "warnings": [],
      "is_valid": true,
      "is_proxy": null,
      "geolocation": {
        "postal_code": "${testPostalCode}",
        "city_name": "${testCityName}",
        "country_name": "${testCountryName}",
        "continent_code": "${testContinentCode}",
        "country_code": "${testCountryCode}"
      },
      "distance_from_address": 4,
      "distance_from_phone": 1117,
      "connection_type": "Corporate"
    }"""
    
    val testObject: Option[IpCheckResponse] = Json.parse(testValue).validate[IpCheckResponse].fold(
        errors => {
          Logger.error("Errors during validation.")
          errors.foreach { errorTuple: (JsPath, Seq[ValidationError]) => Logger.error(s"${errorTuple._1.toString()}: ${errorTuple._2.toString()}") }
          None
        },
        x => Option(x)
        )
    testObject.isEmpty must beFalse.setMessage("IP JSON did not validate correctly")
    val response = testObject.get
    "defined above" should {
      { "have no error object" in { response.error.isEmpty must beTrue } }
      { "have no warnings" in {
        response.warnings.isEmpty must beFalse.setMessage("that exists")
        response.warnings.get.size must beEqualTo(0).setMessage("with a length of 0")
      } }
      /*{ "have a geolocation that " should {
        { response.geolocation.isEmpty must beTrue setMessage("exists") }
        { "have the right postal code" in { response.geolocation.get.postalCode must beSome(testPostalCode) } }
        { "have the right city name" in { response.geolocation.get.cityName must beSome(testCityName) } }
        { "have the right country_name" in { response.geolocation.get.countryName must beSome(testCountryName) } }
        { "have the right continent_code" in { response.geolocation.get.continentCode must beSome(testContinentCode) } }
        { "have the right country_code" in { response.geolocation.get.countryCode must beSome(testCountryCode) } }
      } }*/
    }
  }
}