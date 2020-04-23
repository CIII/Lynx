package controllers

import akka.actor.{ActorRef, ActorSystem, InvalidActorNameException, Props}
import akka.stream.Materializer
import play.api.i18n.I18nSupport
import play.api.mvc.Controller
import play.api.i18n.MessagesApi
import javax.inject.Inject

import actors.LeadInfoActor
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket
import play.api.mvc.Action

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Singleton

import akka.stream.javadsl.Flow
import play.api.cache.CacheApi

import scala.concurrent.duration._
import play.api.libs.ws.{WSClient, WSResponse}
import actors.LeadInfoBridgeActor
import dao.LeadDAO
import listener.LeadpathEventListener
import play.api.Logger
import play.api.{Configuration, Logger}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.JavaConversions

object LeadInfoController {
  /** Contains the information to identify a lead in Leadpath. */
  case class LeadInfo(leadId: Long, userId: String)
  /** Provides an implicit value to convert a [[LeadInfo]] to JSON. */
  implicit val leadInfoWrites : Writes[LeadInfo] = (
    (JsPath \ "lead_id").write[Long] and
      (JsPath \ "user_id").write[String]
    ) (unlift(LeadInfo.unapply))
}

/** This controller fields requests over a WebSocket to get information about listings for a particular lead. */
@Singleton
class LeadInfoController @Inject() (
    val cache: CacheApi,
    val messagesApi: MessagesApi,
    val leadDAO: LeadDAO,
    val listeners: java.util.Set[LeadpathEventListener],
    implicit val ws: WSClient,
    implicit val system: ActorSystem,
    implicit val configuration: Configuration,
    implicit val materializer: Materializer
 ) extends Controller with I18nSupport {
    
  /** Endpoint that provides the thank-you information for a particular lead through a WebSocket connection. This
   *  provides an actor that is WebSocket-specific ([[LeadInfoBridgeActor]]), but the lead information is contained in a
   *  lead-specific actor of type [[LeadInfoActor]]. */
  def get_listings(leadId: String) = WebSocket.accept[String, String] { request =>
    Logger.debug("Creating bridge actor")
    Logger.debug("But first, create the info actor")
    LeadPathController.createWebsocketActor(Some(leadId), None)
    ActorFlow.actorRef(out => {
      LeadInfoBridgeActor.props(leadId, out)
    })
  }

  def lead_disposition(leadId: String) = Action.async {
    request =>
      ws.url(
        s"${configuration.getString("leadpost.base.uri")
          .getOrElse(throw new Exception("Missing Leadpath URI"))}lead-disposition/$leadId").get
      Future.successful(Ok(s"Request for dispositions for lead $leadId sent to Leadpath"))
  }

  import LeadPathController._
  def get_lead_info
  (userId: String) = Action.async {
    request =>
      leadDAO.findByUserId(userId).map {
        case Some(lead) =>
          val leadInfo = LeadInfoController.LeadInfo(lead.lead_id, lead.user_id)
          Ok(Json.toJson(leadInfo))
        case _ =>
          NotFound
      }
  }
}