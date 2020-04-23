package security

import be.objectify.deadbolt.scala.HandlerKey

object HandlerKeys {

  val defaultHandler = Key("defaultHandler")

  case class Key(name: String) extends HandlerKey

}