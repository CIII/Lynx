package modules

import play.api.inject.Module
import play.api.Environment
import play.api.Configuration

object WhitepagesModule {
  val WHITEPAGES_API_KEY: String = "whitepages.api_key"
}

class WhitepagesModule extends Module {
  private val WHITEPAGES_CONFIG_KEY: String = "lynx.whitepages.api_key"
  
  def bindings(environment: Environment, configuration : Configuration) = {
    Seq(bind[String].qualifiedWith(WhitepagesModule.WHITEPAGES_API_KEY).to(configuration.getString(WHITEPAGES_CONFIG_KEY).get))
  }
}