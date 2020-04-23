package models.abtest

import org.joda.time.DateTime

/**
  * Created by slin on 2/3/17.
  */

case class ABTest(
  id: Long,
  name: String,
  var description: Option[String],
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class ABTestMember(
  id: Long,
  event_id: Long,
  session_id: Option[Long],
  ab_test_id: Long,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class ABTestAttribute(
  id: Long,
  name: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)

case class ABTestMemberAttribute(
  id: Long,
  ab_test_member_id: Long,
  ab_test_attribute_id: Long,
  var value: String,
  var created_at: Option[DateTime],
  var updated_at: Option[DateTime]
)