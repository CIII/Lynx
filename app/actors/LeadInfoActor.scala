package actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import play.api.libs.ws.WSResponse
import play.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._

/**
 * This object contains the initialization information to create a LeadInfoActor.
 */
object LeadInfoActor {
  def props(shutdownDuration: Int) = Props(classOf[LeadInfoActor], shutdownDuration: Int)
  val PREFIX = "lead_id:"
  val SHUTDOWN_DURATION_DEFAULT = 20
}

/** 
 *  This package contains the implicits for reading and writing JSON in the format for receiving listings from Leadpath.
 *  This may wiser to break out into multiple packages so that Reads and Writes can be mixed and matched better in the
 *  future to suit the situation.
 */
package object Leadpath {  
  case class LeadpathArrival (id: String, lead: LeadpathLead)
  case class LeadpathLead (id: String, listings: Option[Seq[LeadpathListing]], userInfo: Option[Seq[UserInfo]])
  
  object LeadpathListing {
    def apply(name: String, price: Option[BigDecimal]) = {
      new LeadpathListing(name, price, None, None, None, None, None, Seq.empty)
    }
  }
  case class LeadpathListing (name: String, price: Option[BigDecimal], imageUrl: Option[String], siteUrl: Option[String], rating: Option[BigDecimal], reviewCount: Option[Int], score: Option[Int], descs: Seq[String])

  implicit val leadpathListingReads: Reads[LeadpathListing] = (
    (JsPath \ "code").read[String].orElse((JsPath \ "name").read[String]) and
    (JsPath \ "price").readNullable[BigDecimal]
    )(LeadpathListing.apply (_, _))
    
  // http://stackoverflow.com/questions/15488639/how-to-write-readst-and-writest-in-scala-enumeration-play-framework-2-1
  implicit val SectionTypeReads: Reads[SectionType.SectionType] = Reads[SectionType.SectionType] {
    case JsString(s) => {
      try {
        JsSuccess(SectionType.withName(s))
      } catch {
        case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${SectionType.getClass}', but it does not appear to contain the value: '$s'")
      }
    }
    case _ => JsError("String value expected")
  }

  implicit val userInfoReads : Reads[UserInfo] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "desc").read[String] and
    (JsPath \ "image_url").read[String] and
    (JsPath \ "section_type").read[SectionType.SectionType]
    ) (UserInfo.apply _)

  implicit val leadpathLeadReads: Reads[LeadpathLead] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "listings").readNullable[Seq[LeadpathListing]] and
    (JsPath \ "user_info").readNullable[Seq[UserInfo]]
    )(LeadpathLead.apply _)

  implicit val leadpathArrivalReads: Reads[LeadpathArrival] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "lead").read[LeadpathLead]
    )(LeadpathArrival.apply _)
        
  implicit val userInfoWrites : Writes[UserInfo] = (
      (JsPath \ "title").write[String] and
      (JsPath \ "desc").write[String] and
      (JsPath \ "image_url").write[String] and
      (JsPath \ "section_type").write[SectionType.SectionType]
      ) (unlift(UserInfo.unapply))
    
  implicit val leadpathLeadMatchWrites: Writes[LeadpathListing] = (
      (JsPath \ "title").write[String] and
      (JsPath \ "image_url").writeNullable[String] and
      (JsPath \ "site_url").writeNullable[String] and
      (JsPath \ "rating").writeNullable[BigDecimal] and
      (JsPath \ "review_count").writeNullable[Int] and
      (JsPath \ "score").writeNullable[Int] and
      (JsPath \ "desc").write[Seq[String]]
      )(listing => (listing.name, listing.imageUrl, listing.siteUrl, listing.rating, listing.reviewCount, listing.score, listing.descs))

  implicit val leadpathLeadWrites: Writes[LeadpathLead] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "listing_id").writeNullable[Seq[LeadpathListing]] and
    (JsPath \ "user_info").writeNullable[Seq[UserInfo]]
    )(lead => (lead.id, lead.listings, lead.userInfo))
}

import Leadpath._
import akka.actor.Cancellable

/**
 * An actor of this class brokers information about a lead, including the listings that it was sold to. An instance of
 * this actor is instantiated and named in the Akka Actor system "/user/lead_id:{:leadId}". "/user/" is the default
 * address for user-created actors, and "lead_id:" is the prefix, defined at LeadInfoActor.PREFIX.
 * 
 * @constructor Creates an actor. Do not use; use LeadInfoActor.props() and the ActorSystem#actorOf method.
 */
class LeadInfoActor(val shutdownDuration: Int) extends Actor {  
  var lead: LeadpathLead = null
  var out: ActorRef = null
  
  var shutdownTask: Cancellable = null
  resetShutdown
  
  val userInfo1 = new UserInfo("We just emailed your solar savings report.", """It uses federal data on solar yield, typical costs, and applicable rebates and incentives to project your savings. When you get competitive quotes, your savings can be even greater.""", "/images/thanks-001.png", SectionType.Email)
  val userInfo2 = new UserInfo("We’ll be calling shortly with free quotes.", "We’ve selected 3 reputable installers that will provide more info and customized quotes. Comparing multiple offers yields the best results. In the meantime, see options below.", "/images/thanks-002.png", SectionType.Phone)
  val userInfos = Seq(userInfo1, userInfo2)

  
  /**
   * The method that receives messages to the actor. It can receive either: 1. LeadpathLead, an object that describes
   * the lead as received from Leadpath, 2. ActorRef, the output actor that sends messages to the WebSocket connection,
   * 3. CreatedMessage, a message meaning the WebSocket connection has been created, or 4. String, the input message
   * from the WebSocket connection (this is forwarded from an instance of LeadPathInfoBridge).
   */
  def receive = {
    case lead: LeadpathLead => {
      Logger.info("Received a lead: " + lead.toString())
      resetShutdown
      this.lead = lead
      
      if(out != null && this.lead != null) {
        out ! Json.toJson(this.lead.copy(userInfo = Some(userInfos))).toString()
      }
    }
    case out: ActorRef => {
      Logger.debug("Received an out stream")
      this.out = out
    }
    case msg: CreatedMessage => { // Finished setting up. May be redundant with ActorRef above.
      resetShutdown
      Logger.debug("Looking for cached message")
      if(out != null && lead != null) {
        Logger.debug("Sending cached message")
        out ! Json.toJson(this.lead.copy(userInfo = Some(userInfos))).toString()
      }
    }
    case msg: String => { // Incoming message from browser
      resetShutdown
      Logger.info("Received a message: " + msg)
      println(msg)
      if(out != null) {
        out ! (s"Echo: $msg")
      }
    }
    case default => {
      Logger.info("Did not understand message: " + default.toString())
    }
  }
  
  private def resetShutdown : Unit = {
    try {
      if(this.shutdownTask != null && !this.shutdownTask.isCancelled) {
        Logger.debug("Resetting the actor shutdown.")
        this.shutdownTask.cancel()
      } else {
        Logger.debug("The task is cancelled?")
      }
      this.shutdownTask = this.context.system.scheduler.scheduleOnce(shutdownDuration minutes) {
        Logger.debug("Shutting down actor")
        if(context != null) {
          context.stop(self)
        }
      }
    } catch {
      case e: IllegalStateException => { Logger.debug("The current task is already cancelled, or is un-cancellable because it is already started.")}
      case e: Throwable => {
        Logger.warn("Intercepted an unknown error resetting the shutdown timer of a lead info actor. Rethrowing.", e)
        throw e
      }
    }
  }
}

/**
 * This class is sent as a message to the actor to indicate that a WebSocket connection was created and the cached
 * information should be delivered.
 */
case class CreatedMessage()

case class UserInfo(title: String, description: String, imageUrl: String, sectionType: SectionType.SectionType)

object SectionType extends Enumeration {
  type SectionType = Value
  val Email, Phone = Value
}
