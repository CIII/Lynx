package controllers

import javax.inject.Inject
import api.SessionEvent.PageType
import api.SessionEvent
import dao._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import utils.templates.TemplateSettings
import utils.templates.Settings
import utils.templates.HeaderSettings
import views.js.external.page_render

class ExternalController @Inject()(
val browserDAO: BrowserDAO,
val urlDAO: UrlDAO,
val domainDAO: DomainDAO,
val eventDAO: EventDAO,
val eventTypeDAO: EventTypeDAO,
val attributeDAO: AttributeDAO,
val eventAttributeDAO: EventAttributeDAO,
val sessionDAO: SessionDAO,
val sessionAttributeDAO: SessionAttributeDAO,
val formDAO: FormDAO,
val ws: WSClient,
val messagesApi: MessagesApi,
implicit val environment: play.api.Environment,
implicit val configuration: play.api.Configuration
) extends Controller with I18nSupport with SessionEvent {
  def track = Action.async {
    implicit request => Future.successful(Ok(views.js.external.track()))
  }

  def lynx_reporting(domain: String) = Action.async { implicit request =>
    domain match {
      case "easiersolar" | "homesolar" | "mutualofomahamedicareplans" => Future.successful(Ok(views.js.external.lynx_reporting(HeaderSettings(false))))
      case _ => Future.successful(Ok(views.js.external.lynx_reporting(HeaderSettings(true))))
    }
    
  }

  def wordpress_page_render = Action.async(
    withSessionIDAndSeparatePageRenderEventID{
      (session, event) => {
        implicit request =>
          val pageType = PageType.ThirdParty.toString
          persist_page_type(pageType, event.id.get)
          Ok(page_render())
      }
    }
  )
}
