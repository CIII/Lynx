package modules

import com.google.inject.AbstractModule
import utils.templates.TemplatesStartup

/**
  * Created by slin on 6/18/17.
  */
class TemplatesModule extends AbstractModule{
  def configure(): Unit = {
    play.api.Logger.debug("TemplatesModule: Loading Template Settings")
    bind(classOf[TemplatesStartup]).asEagerSingleton();
  }
}
