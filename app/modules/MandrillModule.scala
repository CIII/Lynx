package modules

import play.api.inject.Module
import play.api.Environment
import play.api.Configuration
import play.api.Logger
import com.tapquality.dao.MandrillDAO
import com.tapquality.dao.impl.MandrillDAO

class MandrillModule extends Module {
  
  def bindings(environment : Environment, configuration : Configuration) = {
    Logger.debug("In MandrillModule#bindings")
    Seq(
        bind[String].qualifiedWith("mandrill.key").to(configuration.getString("lynx.mandrill.key").get),
        bind[String].qualifiedWith("mandrill.url").to(configuration.getString("lynx.mandrill.url").get),
        bind(classOf[com.tapquality.dao.MandrillDAO]).to(classOf[com.tapquality.dao.impl.MandrillDAO]))
  }
}