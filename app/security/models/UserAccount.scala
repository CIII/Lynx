package security.models

import be.objectify.deadbolt.scala.models.{Permission, Role, Subject}

/**
  * Created by slin on 9/5/17.
  */

case class UserAccount(
  id: Long,
  var username: String,
  var password: String
) extends Subject {
  override def identifier: String = username

  override def roles: List[Role] = List()

  override def permissions: List[Permission] = List()
}