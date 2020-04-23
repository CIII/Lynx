package security

import javax.inject.Singleton

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}
import com.google.inject.Inject
import security.dao.UserAccountDAO

@Singleton
class ConfigUIHandlerCache @Inject()(
  val userAccountDAO: UserAccountDAO) extends HandlerCache {

  val defaultHandler: DeadboltHandler = new ConfigUIDeadboltHandler(userAccountDAO)

  val handlers: Map[Any, DeadboltHandler] = Map(
    HandlerKeys.defaultHandler -> defaultHandler
  )

  override def apply(): DeadboltHandler = defaultHandler

  override def apply(handlerKey: HandlerKey): DeadboltHandler = handlers(handlerKey)
}