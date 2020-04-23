package models

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Writes, JsNumber, JsObject, JsValue}


case class AbTest(
  id: Option[Long],
  var name: String,
  var description: Option[String],
  var link: Option[String],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class AbArrival(
  id: Option[Long],
  var browser_id: String,
  var ab_test_id: Option[Long],
  var arrival_form_id: Option[Long],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Attribute(
  id: Option[Long],
  var name: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Domain(
  id: Option[Long],
  var domain: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class TrafficSource(
  id: Option[Long],
  var traffic_type_id: Option[Long],
  var name: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class TrafficType(
  id: Option[Long],
  var name: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Browser(
  id: Option[Long],
  var browser_id: Option[String],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class BrowserEndpoint(
  id: Option[Long],
  var browser_id: Option[Long],
  var endpoint_id: Option[Long],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Endpoint(
  id: Option[Long],
  var ip_address: Option[String],
  var ip_normalized: Option[String],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class EventType(
  id: Option[Long],
  var name: String,
  var event_category: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Url(
  id: Option[Long],
  var url: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Event(
  id: Option[Long],
  var event_type_id: Option[Long],
  var session_id: Option[Long],
  var parent_event_id: Option[Long],
  var url_id: Option[Long],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class Form(
  id: Option[Long],
  var session_id: Option[Long],
  var event_id: Option[Long],
  var first_name: Option[String],
  var last_name: Option[String],
  var full_name: Option[String],
  var email: String,
  var street: Option[String],
  var city: Option[String],
  var state: Option[String],
  var zip: Option[String],
  var property_ownership: Option[String],
  var electric_bill: Option[String],
  var electric_company: Option[String],
  var phone_home: Option[String],
  var leadid_token: Option[String],
  var domtok: Option[String],
  var ref: Option[String],
  var xxTrustedFormToken: Option[String],
  var xxTrustedFormCertUrl: Option[String],
  var dob: Option[DateTime],
  var post_status: Option[Int]
)

case class Session(
  id: Option[Long],
  var browser_id: Option[Long],
  var domain_id: Option[Long],
  var ip: Option[String],
  var user_agent: Option[String],
  var traffic_source_id: Option[Long],
  var is_robot: Boolean,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime],
  var last_activity: Option[DateTime]
)

case class EventAttribute(
  id: Option[Long],
  var event_id: Option[Long],
  var attribute_id: Option[Long],
  var value: Option[String],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class SessionAttribute(
 id: Option[Long],
 var session_id: Option[Long],
 var attribute_id: Option[Long],
 var value: Option[String],
 var created_at: Option[DateTime],
 var updated_at: Option[DateTime]
)

case class Revenue(
  id: Long,
  var session_id: Long,
  var total_revenue: BigDecimal,
  var con_f: Long,
  var created_at: DateTime,
  var updated_at: DateTime
)

case class Lead(
  lead_id: Long,
  user_id: String,
  session_id: Long,
  var created_at: DateTime,
  var updated_at: DateTime
)

/**********************
          API
**********************/

case class ApiToken(
  id: Option[Long],
  token: String,
  var active: Boolean,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)