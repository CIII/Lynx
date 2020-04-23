import play.sbt.PlayRunHook
import sbt._
import java.net.InetSocketAddress

object Grunt {
  def apply(base: File): PlayRunHook = {

    object GruntProcess extends PlayRunHook {

      var watchProcess: Option[Process] = None

      override def beforeStarted(): Unit = {
        if(System.getProperty("os.name").startsWith("Windows")){
          Process("cmd /c grunt dev", base).run
        } else {
          Process("grunt dev", base).run
        }
      }

      override def afterStarted(addr: InetSocketAddress): Unit = {
        if(System.getProperty("os.name").startsWith("Windows")){
          watchProcess = Some(Process("cmd /c grunt watch", base).run)
        } else {
          watchProcess = Some(Process("grunt watch", base).run)
        }
      }

      override def afterStopped(): Unit = {
        watchProcess.map(p => p.destroy())
        watchProcess = None
      }
    }

    GruntProcess
  }
}