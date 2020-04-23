package modules

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import listener.{LeadpathEventListener, SendToWebSocketListener, AggregatePriceListener}
import play.api.Logger

class LynxModule extends AbstractModule {
  def configure() = {
    try {
      val leadpathEventListeners : Multibinder[LeadpathEventListener] = Multibinder.newSetBinder(binder, classOf[LeadpathEventListener])
      leadpathEventListeners.addBinding().to(classOf[AggregatePriceListener])
      leadpathEventListeners.addBinding().to(classOf[SendToWebSocketListener])
    } catch {
    case e : Throwable => {
        Logger.debug(e.toString())
      }
    }
  }
}