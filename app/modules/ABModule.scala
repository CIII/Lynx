package modules

import com.google.inject.AbstractModule
import abTest.ABTestStartup

/**
  * Created by slin on 6/14/17.
  */
class ABModule extends AbstractModule{
  def configure(): Unit ={
    play.api.Logger.debug("ABTestModule: Compiling Tests")
    bind(classOf[ABTestStartup]).asEagerSingleton();
  }
}
