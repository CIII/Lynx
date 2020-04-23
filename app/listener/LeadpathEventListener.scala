package listener

import actors.Leadpath._
import scala.concurrent.Future

/** Defines the interface to process messages from Leadpath **/
trait LeadpathEventListener {
  /** The method that must be implemented to process messages from Leadpath. The [[Future]] is completed when the
   *  message from Leadpath is processed; this allows for messages to be processed in parallel. Consequently, event
   *  listeners should not be dependent on other event listeners to have completed or not to be operating in parallel.
   **/
  def processMessage(message: LeadpathArrival) : Future[Unit]
}