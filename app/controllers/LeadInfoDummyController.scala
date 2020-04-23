package controllers

import play.api.mvc.Action
import play.api.mvc.WebSocket
import play.api.libs.streams.ActorFlow
import akka.stream.Materializer
import akka.actor.ActorSystem
import javax.inject.Inject
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** This endpoint is a substitute for [[LeadInfoController]] that will log and provide stand-in values for testing the
 *  browser-side of this interaction. It is no longer used. */
class LeadInfoDummyController  @Inject() (implicit system: ActorSystem, materializer: Materializer) {
  def get_listings(leadId: String) = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => LeadInfoDummyActor.props(out))
  }
}

package object Implicits {
  import SectionType._
  
  implicit val dummyUserInfoWrites : Writes[DummyUserInfo] = (
      (JsPath \ "title").write[String] and
      (JsPath \ "desc").write[String] and
      (JsPath \ "image_url").write[String] and
      (JsPath \ "section_type").write[SectionType]
      ) (unlift(DummyUserInfo.unapply))
          
  implicit val dummyListingWrites : Writes[DummyListing] = (
      (JsPath \ "title").write[String] and
      (JsPath \ "descriptions").writeNullable[Seq[String]] and
      (JsPath \ "image_url").writeNullable[String] and
      (JsPath \ "site_url").writeNullable[String] and
      (JsPath \ "rating").writeNullable[BigDecimal] and
      (JsPath \ "review_count").writeNullable[Int] and
      (JsPath \ "score").writeNullable[Int] and 
      (JsPath \ "type").write[String]
      )(listing => (listing.name, listing.descriptions, listing.imageUrl, listing.siteUrl, listing.rating, listing.reviewCount, listing.score, listing.listingType))
  
  implicit val dummyMessageWrites : Writes[DummyMessage] = (
      (JsPath \ "id").write[String] and
      (JsPath \ "vendors").write[Seq[DummyListing]] and
      (JsPath \ "user_info").write[Seq[DummyUserInfo]]
      ) (unlift(DummyMessage.unapply))
}
import controllers.Implicits._

object LeadInfoDummyActor {
  def props(out : ActorRef) = Props(new LeadInfoDummyActor(out))
}

class LeadInfoDummyActor(val out : ActorRef) extends Actor {
  import SectionType._
  
  val listing1 = new DummyListing("vendor1", Some(Seq("desc 1 1", "desc 1 2", "desc 1 3")), Some("image url 1"), Some("site url 1"), Some(BigDecimal.decimal(10.0)), Some(11), Some(12), "advertisement")
  val listing2 = new DummyListing("vendor2", Some(Seq("desc 2 1", "desc 2 2", "desc 2 3")), Some("image url 2"), Some("site url 2"), Some(BigDecimal.decimal(20.0)), Some(21), Some(22), "listing")
  val listings = Seq(listing1, listing2)
  val userInfo1 = new DummyUserInfo("Title 1", "Description 1", "Image URL 1", SectionType.Phone)
  val userInfo2 = new DummyUserInfo("Title 2", "Description 2", "Image URL 2", SectionType.Email)
  val userInfos = Seq(userInfo1, userInfo2)
  val outMessage = Json.toJson(new DummyMessage("3", listings, userInfos)).toString()
  out ! outMessage

  def receive = {
    case msg : String => {
      out ! outMessage
    }
  }
}

case class DummyMessage(id : String, listings : Seq[DummyListing], userInfos : Seq[DummyUserInfo])
case class DummyListing(name: String, descriptions: Option[Seq[String]], imageUrl: Option[String], siteUrl: Option[String], rating: Option[BigDecimal], reviewCount: Option[Int], score: Option[Int], listingType: String)
case class DummyUserInfo(title: String, description: String, imageUrl: String, sectionType: SectionType.SectionType)

object SectionType extends Enumeration {
  type SectionType = Value
  val Email, Phone = Value
}
