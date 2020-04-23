package listener

import java.util.concurrent.TimeUnit

import actors.Leadpath.LeadpathArrival

import scala.concurrent.Future
import akka.actor.ActorSystem
import actors.LeadInfoActor
import play.Logger
import javax.inject.Inject

import dao.ListingsDAO
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class SendToWebSocketListener @Inject()(val system: ActorSystem, val listingsDAO: ListingsDAO) extends LeadpathEventListener {
  
  /** Returns a [[Future]] that is completed when the message from Leadpath has been added to the collection of actors
   *  in such a way that [[actors.LeadInfoBridgeActor]] can find it and process it when the browser calls back to this
   *  server.
   *  @todo I think this is implemented wrong. It should put the processing in a [[Future]], not do the processing and then return a successful Future. **/
      def processMessage(message: LeadpathArrival) : Future[Unit] = {
        Logger.debug(s"Starting processing message to websocket")
        val actor = system.actorSelection("/user/" + LeadInfoActor.PREFIX + message.lead.id)
        val lead = message.lead
        val listings = lead.listings
        if(!lead.listings.isEmpty) {
          val listingsFuture = listingsDAO.populateListings(lead.listings.get)
          listingsFuture.map { listings =>
            actor ! message.lead.copy(listings = Some(listings)) }
        } else {
          actor ! message.lead
        }
        Logger.debug(s"Sent message to actor")
        Future.successful(Unit)
    }
}
