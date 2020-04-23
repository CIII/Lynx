package actors

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import api.solar.calculator.SolarCalculatorService
import play.Logger
import controllers.LeadPathController
import dao._
import listener.LeadpathEventListener
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import utils.EmailCommand
import com.google.inject.Inject

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * This object contains the initialization information to create a LeadInfoBridgeActor.
 * 
 * @author Jonathan Card
 */
object LeadInfoBridgeActor {
  def props(leadInfoName: String, out: ActorRef) = Props(classOf[LeadInfoBridgeActor], leadInfoName, out)
}

/**
 * An actor of this class is instantiated for each connection for a WebSocket. It acts as a link between the WebSocket
 * connection and the actor aggregating the information about the lead, of class {@link actors.LeadInfoActor}
 * 
 * @constructor creates an actor that bridges a WebSocket connection with a multi-threaded broker of lead information.
 * @param leadId The ID of the lead being inquired about
 * @param out The output stream that ultimately leads to the WebSocket and the browser
 */
class LeadInfoBridgeActor(val leadId: String, val out: ActorRef) extends Actor {
  Logger.info("Context: " + context.self.path.toString());

  // TODO: Currently, if there is no such actor, these messages will go int the ether, and there will be no error and no data. If it does not exist, create and request data from Leadpath.
  val leadInfoActor = context.actorSelection(s"/user/${LeadInfoActor.PREFIX}${leadId}")
  Logger.info("Created a LeadInfoBridgeActor, address: " + leadInfoActor.pathString)
  leadInfoActor ! out
  leadInfoActor ! new CreatedMessage
  Logger.info("Past the created message")
  
  def receive = {
    case msg: String => {
      leadInfoActor ! msg
    }
  }  
}