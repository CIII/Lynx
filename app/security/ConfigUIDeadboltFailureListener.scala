package security

import javax.inject.Singleton

import be.objectify.deadbolt.scala.TemplateFailureListener
import play.api.Logger

@Singleton
class ConfigUIDeadboltFailureListener extends TemplateFailureListener {

  val logger: Logger = Logger("deadbolt.template")

  override def failure(message: String, timeout: Long): Unit = logger.error(s"Bad things! Message [$message]  timeout [$timeout]ms")
}
