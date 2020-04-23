package controllers

import javax.inject.Inject

import api.PsuedoStatic
import play.api.mvc._
import play.twirl.api.Html
import utils.templates.{HeaderSettings, Settings, TemplateSettings}

import scala.concurrent.Future
import play.api.Logger

class PartialViewsController @Inject()(
  implicit configuration: play.api.Configuration,
  implicit val environment: play.api.Environment
) extends Controller with PsuedoStatic{

  def header(domain: String = "easiersolar", directory: String = "home_page") = Action.async(
    withCacheControl{
      request =>
        val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain, directory=directory)
        Ok(views.html.angular.header.header(siteSettings, HeaderSettings(false)))
  })

  def home(domain: String = "easiersolar", directory: String = "home_page") = Action.async(
    withCacheControl {
      request =>
        val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain, directory=directory)
        Ok(views.html.angular.home.home(landingSettings, siteSettings))
  })

  def form(domain: String = "easiersolar", form: String) = Action.async(
    withCacheControl {
    request =>
      val formPattern = """(.+).html""".r
      val Settings(homeSettings,siteSettings) = TemplateSettings.get_settings(domain=domain)
      val formResult = form match {
        case formPattern(formCode) =>
          try {
            val clazz: Class[_] = Class.forName(s"views.html.angular.forms.${formCode}");
            val render: java.lang.reflect.Method = clazz.getDeclaredMethod("render", siteSettings.getClass, configuration.getClass, environment.getClass);
            Ok(render.invoke(null, siteSettings, configuration, environment).asInstanceOf[Html])
          }catch {
            case e: Throwable =>
              play.api.Logger.error(s"[PartialView] This is a form that does not exist yet, ${form}", e)
              NotFound("This is a form that does not exist yet")
          }
        case _ =>
          play.api.Logger.error(s"[PartialView] This is a form that does not exist yet, ${form}")
          NotFound("This is a form that does not exist yet")
      }

      formResult
  })

  def ng_app(domain: String = "easiersolar", directory: String = "home_page") = Action.async(
    withCacheControl {
    request =>
      val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory=directory)
      Ok(views.js.angular.app(landingSettings, siteSettings))
  })

  def ng_home_controller(domain: String = "easiersolar", directory: String = "home_page") = Action.async(
    withCacheControl {
    request =>
      val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory=directory)
      Ok(views.js.angular.controllers.home_controller(landingSettings))
  })
}