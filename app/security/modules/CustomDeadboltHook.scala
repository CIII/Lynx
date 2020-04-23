package security.modules

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{DeadboltExecutionContextProvider, TemplateFailureListener}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import security.{ConfigUIDeadboltExecutionContextProvider, ConfigUIDeadboltFailureListener, ConfigUIHandlerCache}

class CustomDeadboltHook extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[TemplateFailureListener].to[ConfigUIDeadboltFailureListener],
    bind[HandlerCache].to[ConfigUIHandlerCache],
    bind[DeadboltExecutionContextProvider].to[ConfigUIDeadboltExecutionContextProvider]
  )
}
