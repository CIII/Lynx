package dao

import play.api.libs.json.Reads
import play.api.libs.json.JsPath
import play.api.libs.functional.syntax._
import play.Logger
import play.api.libs.ws.WSClient
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import dao.whitepages._
import play.api.data.validation.ValidationError
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.concurrent.Future
import javax.inject.Named
import javax.inject.Inject
import play.api.libs.ws.WSResponse

class WhitepagesDAO @Inject()(@Named("whitepages.api_key") apiKey: String) {
  private val URL = "https://proapi.whitepages.com/3.3/lead_verify.json"
  
  def query(request: WhitepagesRequest)(implicit ws: WSClient) : Future[Option[WhitepagesResponse]] = {
    val parameterNames = (
        "name",
        "firstname",
        "lastname",
        "phone",
        "email_address",
        "ip_address",
        "address.street_line_1",
        "address.street_line_2",
        "address.city",
        "address.post_code",
        "address.state_code",
        "address.country_code")
        
    val parameters = (parameterNames.productIterator zip (Function.unlift(WhitepagesRequest.unapply)(request)).productIterator).toList.asInstanceOf[Seq[(String, String)]]
    Logger.debug((parameters :+ ("api_key", apiKey)).toString)
    val response = ws.url(URL).withFollowRedirects(true).withQueryString(parameters :+ ("api_key", apiKey) : _*).get()
    val responseFuture = response.map { response =>
      Json.parse(response.body).validate[WhitepagesResponse].fold(
        handleValidationError(response),
        responseObj => {
          Some(responseObj)
      })
    }
    return responseFuture
  }
  
  private def handleValidationError(response: WSResponse): Seq[(JsPath, Seq[ValidationError])] => Option[WhitepagesResponse] = {
    { errors: Seq[(JsPath, Seq[ValidationError])] =>
      Logger.error(s"Error parsing response from whitepages.com. Discarding results: ${response.body}")
      errors.foreach { errorTuple: (JsPath, Seq[ValidationError]) => Logger.error(s"${errorTuple._1.toString()}: ${errorTuple._2.toString()}") }
      None
    }
  }
}

case class WhitepagesRequest(
    name: String,
    firstname: String,
    lastname: String,
    phone: String,
    emailAddress: String,
    ipAddress: String,
    addressStreet1: String,
    addressStreet2: String,
    addressCity: String,
    addressPostalCode: String,
    addressStateCode: String,
    addressCountryCode: String
    )
    
case class WhitepagesResponse(
    error: Option[WhitepagesErrorMessage],
    phoneCheck: Option[PhoneCheckResponse],
    addressCheck: Option[AddressCheckResponse],
    emailCheck: Option[EmailCheckResponse],
    ipCheck: Option[IpCheckResponse]
    )

case class PhoneCheckResponse(
    error: Option[WhitepagesErrorMessage],
    warnings: Option[Seq[String]],
    isValid: Option[Boolean],
    phoneContactScore: Option[Int],
    isConnected: Option[Boolean],
    phoneToName: Option[String],
    subscribeName: Option[String],
    subscriberAgeRange: Option[String],
    subscriberGender: Option[String],
    subscriberAddress: Option[WhitepagesAddressResponse],
    countryCode: Option[String],
    lineType: Option[String],
    carrier: Option[String],
    isPrepaid: Option[Boolean],
    isCommercial: Option[Boolean]
    )
    
case class AddressCheckResponse(
    error: Option[WhitepagesErrorMessage],
    warnings: Option[Seq[String]],
    isValid: Option[Boolean],
    diagnostics: Option[Seq[String]],
    addressContactScore: Option[Int],
    isActive: Option[Boolean],
    addressToName: Option[String],
    residentName: Option[String],
    residentAgeRange: Option[String],
    residentGender: Option[String],
    addressType: Option[String],
    isCommercial: Option[Boolean],
    residentPhone: Option[String]
    )
    
case class EmailCheckResponse(
    error: Option[WhitepagesErrorMessage],
    warnings: Option[Seq[String]],
    isValid: Option[Boolean],
    diagnostics: Option[Seq[String]],
    emailContactScore: Option[Int],
    isDisposable: Option[Boolean],
    emailToName: Option[String],
    registeredName: Option[String]
    )

case class IpCheckResponse(
    error: Option[WhitepagesErrorMessage],
    warnings: Option[Seq[String]],
    isValid: Option[Boolean],
    isProxy: Option[Boolean],
    geolocation: Option[Geolocation],
    distanceFromAddress: Option[Int],
    distanceFromPhone: Option[Int],
    connectionType: Option[String]
    )
    
case class WhitepagesErrorMessage(message: String, name: String)
case class WhitepagesAddressResponse(streetLine1: Option[String], streetLine2: Option[String], city: Option[String], postalCode: Option[String], stateCode: Option[String], countryCode: Option[String])
case class Geolocation(postalCode: Option[String], cityName: Option[String], subdivision: Option[String], countryName: Option[String], continentCode: Option[String], countryCode: Option[String])

object whitepages {
  
  implicit val whitepagesErrorMessageReads: Reads[WhitepagesErrorMessage] = (
      (JsPath \ "message").read[String] and
      (JsPath \ "name").read[String]
      )(WhitepagesErrorMessage.apply _)
      
  implicit val whitepagesAddressResponseReads: Reads[WhitepagesAddressResponse] = (
      (JsPath \ "street_line_1").readNullable[String] and
      (JsPath \ "street_line_2").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "postal_code").readNullable[String] and
      (JsPath \ "state_code").readNullable[String] and
      (JsPath \ "country_code").readNullable[String]
      )(WhitepagesAddressResponse.apply _)
      
  implicit val geolocationReads: Reads[Geolocation] = (
      (JsPath \ "postal_code").readNullable[String] and
      (JsPath \ "city_name").readNullable[String] and
      (JsPath \ "subdivision").readNullable[String] and
      (JsPath \ "country_name").readNullable[String] and
      (JsPath \ "continent_code").readNullable[String] and
      (JsPath \ "country_code").readNullable[String]
      )(Geolocation.apply _)
      
  implicit val phoneCheckResponseReads: Reads[PhoneCheckResponse] = (
      (JsPath \ "error").readNullable[WhitepagesErrorMessage] and
      (JsPath \ "warnings").readNullable[Seq[String]] and
      (JsPath \ "is_valid").readNullable[Boolean] and
      (JsPath \ "phone_contact_score").readNullable[Int] and
      (JsPath \ "is_connected").readNullable[Boolean] and
      (JsPath \ "phone_to_name").readNullable[String] and
      (JsPath \ "subscriber_name").readNullable[String] and
      (JsPath \ "subscriber_age_range").readNullable[String] and
      (JsPath \ "subscriber_gender").readNullable[String] and
      (JsPath \ "subscriber_address").readNullable[WhitepagesAddressResponse] and
      (JsPath \ "country_code").readNullable[String] and
      (JsPath \ "line_type").readNullable[String] and
      (JsPath \ "carrier").readNullable[String] and
      (JsPath \ "is_prepaid").readNullable[Boolean] and
      (JsPath \ "is_commercial").readNullable[Boolean]
      )(PhoneCheckResponse.apply _)
      
  implicit val addressCheckResponseReads: Reads[AddressCheckResponse] = (
      (JsPath \ "error").readNullable[WhitepagesErrorMessage] and
      (JsPath \ "warnings").readNullable[Seq[String]] and
      (JsPath \ "is_valid").readNullable[Boolean] and
      (JsPath \ "diagnostics").readNullable[Seq[String]] and
      (JsPath \ "address_contact_score").readNullable[Int] and
      (JsPath \ "is_active").readNullable[Boolean] and
      (JsPath \ "address_to_name").readNullable[String] and
      (JsPath \ "resident_name").readNullable[String] and
      (JsPath \ "resident_age_range").readNullable[String] and
      (JsPath \ "resident_gender").readNullable[String] and
      (JsPath \ "type").readNullable[String] and
      (JsPath \ "is_commercial").readNullable[Boolean] and
      (JsPath \ "resident_phone").readNullable[String]
      )(AddressCheckResponse.apply _)
      
  implicit val emailCheckResponseReads: Reads[EmailCheckResponse] = (
      (JsPath \ "error").readNullable[WhitepagesErrorMessage] and
      (JsPath \ "warnings").readNullable[Seq[String]] and
      (JsPath \ "is_value").readNullable[Boolean] and
      (JsPath \ "diagnostics").readNullable[Seq[String]] and
      (JsPath \ "email_contact_score").readNullable[Int] and
      (JsPath \ "is_disposable").readNullable[Boolean] and
      (JsPath \ "email_to_name").readNullable[String] and
      (JsPath \ "registered_name").readNullable[String]
      )(EmailCheckResponse.apply _)
      
  implicit val ipChecksResponseReads: Reads[IpCheckResponse] = (
      (JsPath \ "error").readNullable[WhitepagesErrorMessage] and
      (JsPath \ "warnings").readNullable[Seq[String]] and
      (JsPath \ "is_valid").readNullable[Boolean] and
      (JsPath \ "is_proxy").readNullable[Boolean] and
      (JsPath \ "geolocation").readNullable[Geolocation] and
      (JsPath \ "distance_from_address").readNullable[Int] and
      (JsPath \ "distance_from_phone").readNullable[Int] and
      (JsPath \ "connection_type").readNullable[String]
      )(IpCheckResponse.apply _)
      
  implicit val whitepagesResponseReads: Reads[WhitepagesResponse] = (
      (JsPath \ "error").readNullable[WhitepagesErrorMessage] and
      (JsPath \ "phone_checks").readNullable[PhoneCheckResponse] and
      (JsPath \ "address_checks").readNullable[AddressCheckResponse] and
      (JsPath \ "email_address_checks").readNullable[EmailCheckResponse] and
      (JsPath \ "ip_address_checks").readNullable[IpCheckResponse]
      )(WhitepagesResponse.apply _)
}
