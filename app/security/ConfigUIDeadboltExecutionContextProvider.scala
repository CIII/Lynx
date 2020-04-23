package security

import be.objectify.deadbolt.scala.DeadboltExecutionContextProvider

import scala.concurrent.ExecutionContext

class ConfigUIDeadboltExecutionContextProvider extends DeadboltExecutionContextProvider {
  override def get(): ExecutionContext = scala.concurrent.ExecutionContext.global
}

