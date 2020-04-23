package utils

import javax.inject._

import play.api.mvc.RequestHeader
import play.api.http.HttpConfiguration
import play.api.http.DefaultHttpRequestHandler
import play.api.http.HttpErrorHandler
import play.api.http._
import utils.templates.TemplateSettings

import scala.util.matching.Regex

class MultiSiteRequestHandler @Inject() (errorHandler: HttpErrorHandler,
                                         configuration: HttpConfiguration,
                                         filters: HttpFilters,
                                         easierSolarRouter: easiersolar.Routes,
                                         homeSolarRouter: homesolar.Routes,
                                         mutualOfOmahaRouter: mutualofomahamedicareplans.Routes,
                                         tapxsRouter: tapxs.Routes,
                                         implicit val playConfig: play.api.Configuration)
  extends DefaultHttpRequestHandler(easierSolarRouter, errorHandler, configuration, filters) {

  override def routeRequest(request: RequestHeader) = {

    val domain = utils.utilities.get_domain_name()(request)
    val templateConfiguration = TemplateSettings.TEMPLATE_CONFIGURATION
    domain match {
      case _ if domain.contains(templateConfiguration.getString("lynx.domains.easiersolar.home_page.host").get) =>
        easierSolarRouter.routes.lift(request)
      case _ if domain.contains(templateConfiguration.getString("lynx.domains.homesolar.home_page.host").get) =>
        homeSolarRouter.routes.lift(request)
      case _ if domain.contains(templateConfiguration.getString("lynx.domains.mutualofomahamedicareplans.home_page.host").get) =>
        mutualOfOmahaRouter.routes.lift(request)
      case _ if domain.contains(templateConfiguration.getString("lynx.domains.tapxs.home_page.host").get) =>
        tapxsRouter.routes.lift(request)
      case _ =>
        super.routeRequest(request)
    }

  }
}