package controllers

import javax.inject.Inject

import abTest.{ABTestLoggingService, ABTestParticipant, ABTestService}
import api.SessionEvent
import api.solar.calculator.SolarCalculatorService
import dao._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc._
import utils.templates._
import utils.utilities._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import SessionEvent.PageType
import be.objectify.deadbolt.scala.ActionBuilders

object PageController {
  val STATES = Map(
    "alabama" -> "al",
    "alaska" -> "ak",
    "arizona" -> "az",
    "arkansas" -> "ar",
    "california" -> "ca",
    "colorado" -> "co",
    "connecticut" -> "ct",
    "delaware" -> "de",
    "florida" -> "fl",
    "georgia" -> "ga",
    "hawaii" -> "hi",
    "idaho" -> "id",
    "illinois" -> "il",
    "indiana" -> "in",
    "iowa" -> "ia",
    "kansas" -> "ks",
    "kentucky" -> "ky",
    "louisiana" -> "la",
    "maine" -> "me",
    "maryland" -> "md",
    "massachusetts" -> "ma",
    "michigan" -> "mi",
    "minnesota" -> "mn",
    "mississippi" -> "ms",
    "missouri" -> "mo",
    "montana" -> "mt",
    "nebraska" -> "ne",
    "nevada" -> "nv",
    "new hampshire" -> "nh",
    "new jersey" -> "nj",
    "new mexico" -> "nm",
    "new york" -> "ny",
    "north carolina" -> "nc",
    "north dakota" -> "nd",
    "ohio" -> "oh",
    "oklahoma" -> "ok",
    "oregon" -> "or",
    "pennsylvania" -> "pa",
    "rhode island" -> "ri",
    "south carolina" -> "sc",
    "south dakota" -> "sd",
    "tennessee" -> "tn",
    "texas" -> "tx",
    "utah" -> "ut",
    "vermont" -> "vt",
    "virginia" -> "va",
    "washington" -> "wa",
    "west virginia" -> "wv",
    "wisconsin" -> "wi",
    "wyoming" -> "wy"
  )
  val ABBREV = STATES.map(_.swap)
}

class PageController @Inject()(
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
  val solarCalculatorService: SolarCalculatorService,
  val abTestLoggingService: ABTestLoggingService,
  val abTestService: ABTestService,
  val actionBuilder: ActionBuilders,
  implicit val environment: play.api.Environment,
  implicit val configuration: play.api.Configuration
) extends Controller with I18nSupport with SessionEvent with ABTestParticipant {

  def index(domain: String) = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.Index.toString
    persist_page_type(pageType, event.id.get)
    Ok(views.html.compare.c160802(EasierSolarFormMapping))
  }
  }
  )

  def editorial = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.Editorial.toString
    persist_page_type(pageType, event.id.get)
    Ok(views.html.editorial())
  }
  }
  )

  def index_new(domain: String = "easiersolar") = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.Index_NEW.toString
    persist_page_type(pageType, event.id.get)
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain)
    val abOverrideParams = getOverrideParams
    domain match {
      case "easiersolar" =>
        Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
      case "homesolar" =>
        Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
      case _ =>
        Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
    }
      }
    }
  )

  def solar_city_deals(domain: String = "easiersolar") = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.SolarCityDeals.toString
    val abOverrideParams = getOverrideParams
    persist_page_type(pageType, event.id.get)
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="solar_city_deals")
    Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
  }
  }
  )

  def solar_rebates(domain: String = "easiersolar", state_srec_credits: String) = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    var pageType: String = null
    val abOverrideParams = getOverrideParams

    val srecPattern = """(..)-srec-credits""".r

    val (landingSettings: LandingSettings, siteSettings: SiteSettings) =
      state_srec_credits match {
        case srecPattern(state) =>
          pageType = PageType.SolarRebates.toString
          val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="solar_rebates")
          landingSettings.targetState = Some(state.toLowerCase)
          (landingSettings, siteSettings)
        case _ =>
          pageType = PageType.Index_NEW.toString
          val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="home_page")
          (landingSettings,siteSettings)
      }

    persist_page_type(pageType, event.id.get)

    Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
  }
  }
  )

  def financing(domain: String = "easiersolar", state_solar_loans: String) = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>

    val loansPattern = """(.+)-solar-loans""".r
    val abOverrideParams = getOverrideParams
    var pageType: String = null
    val (landingSettings: LandingSettings, siteSettings: SiteSettings) =
      state_solar_loans match {
        case loansPattern(state_full) =>
          pageType = PageType.Financing.toString
          val state = PageController.STATES(state_full.toLowerCase)
          val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="financing")
          landingSettings.targetState = Some(state)
          (landingSettings, siteSettings)
        case _ =>
          pageType = PageType.Index_NEW.toString
          val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="home_page")
          (landingSettings,siteSettings)
      }

    persist_page_type(pageType, event.id.get)

    Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
  }
  }
  )

  def index_city(domain: String = "easiersolar", city: String) = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.City.toString
    val abOverrideParams = getOverrideParams
    persist_page_type(pageType, event.id.get)
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="city")
    landingSettings.targetCity = Some(city)
    Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
  }
  }
  )

  def electricity_savings(domain: String = "homesolar") = Action.async(
    withSessionIDAndNewEventID {(session, event) =>
      val domain_id = {
        var domain_id = Some(0L)
        for ((fullDomain, id) <- SessionEvent.DOMAINS) {
          if (fullDomain.contains(domain)) domain_id = Some(id)
        }
        domain_id
      }
      withABTestOnPageServe(session,event, domain_id, PageType.ElectricitySavings.toString) { (pageType) => {
        implicit request =>
          val abOverrideParams = getOverrideParams
          persist_page_type(pageType, event.id.get)
          val Settings(landingSettings, siteSettings) = TemplateSettings.get_settings(domain=domain, directory=pageType)
          Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
        }
      }
    }
  )

  def brand(domain: String = "homesolar", brand: String) = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.Installers.toString
    val abOverrideParams = getOverrideParams
    persist_page_type(pageType, event.id.get)
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="brand")
    landingSettings.targetBrand = Some(brand)
    Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
  }
  }
  )

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }

  def getQuotes(domain: String = "easiersolar") = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.GetQuotes.toString
    val abOverrideParams = getOverrideParams
    persist_page_type(pageType, event.id.get)
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="get_quotes")

    domain match {
      case "easiersolar" =>
        Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
      case "homesolar" =>
        Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
      case _ =>
        Ok(views.html.angular.index(landingSettings, siteSettings, abOverrideParams))
    }
      }
    }
  )

  def getQuotesTest = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>

    val headers = List("New 2016 Solar Company Programs", "FIND OUT IF YOU QUALIFY NOW!")
    val settings = GetQuotesSettings(headers, "images/House Hero 1.jpg")

    Ok(views.html.get_quotes_test(EasierSolarFormMapping, settings))
  }
  }
  )

  def compare(id: Long) = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>

      if (id == 160801) {
        var headers = List("New $0 Down Solar Programs", "SEE IF YOU QUALIFY NOW!")
        var hero_image_path = "images/151214/background_image-2.jpg"
        val settings = GetQuotesSettings(headers, hero_image_path)
        val pageType = PageType.Compare160801.toString
        persist_page_type(pageType, event.id.get)
        Ok(views.html.get_quotes(EasierSolarFormMapping, settings))
      } else if (id == 160802) {
        val pageType = PageType.Compare160802.toString
        persist_page_type(pageType, event.id.get)
        Ok(views.html.compare.c160802(EasierSolarFormMapping))
      } else {
        val pageType = PageType.Index.toString
        persist_page_type(pageType, event.id.get)
        Ok(views.html.index(EasierSolarFormMapping))
      }
    }
    })

  def thank_you() = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.ThankYou.toString
    persist_page_type(pageType, event.id.get)
    Ok(views.html.thank_you())
  }
  })

  def installers() = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.Installers.toString
    persist_page_type(pageType, event.id.get)
    Ok(views.html.installers())
  }
  })

  def privacy_policy(domain: String = "easiersolar") = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.PrivacyPolicy.toString
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain)
    persist_page_type(pageType, event.id.get)
    Ok(views.html.privacy_policy(siteSettings))
  }
  })

  def terms_of_use(domain: String = "easiersolar") = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.TermOfUse.toString
    val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain)
    persist_page_type(pageType, event.id.get)
    Ok(views.html.terms_of_use(siteSettings))
  }
  })

  def noho() = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>

    Ok(views.html.noho())
  }
  })

  def middle_class_solar_incentive() = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.MiddleClassSolarIncentive.toString
    persist_page_type(pageType, event.id.get)
    Ok(views.html.educational.easiersolar_test_template1.middle_class_solar_incentive())
  }
  })

  def energy_savings(domain: String = "homesolar") = Action.async(withSessionIDAndNewEventID { (session, event) => { implicit request =>
    val pageType = PageType.EnergySavings.toString
    val Settings(_,siteSettings) = TemplateSettings.get_settings(domain=domain)
    persist_page_type(pageType, event.id.get)
    Ok(views.html.mass_policy.mass_policy(siteSettings))
  }
  })

  def boo(request: Request[AnyContent]): Future[Result] = {
    Future(Ok(""))
  }

  def energy_savings_quotes(domain: String = "homesolar") = Action.async(
    withSessionIDAndCurrentEventID {
      (session, event) => {
        implicit request =>
          val pageType = PageType.EnergySavings.toString
          val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain)
          Ok(views.html.angular.index(landingSettings, siteSettings, None))
      }
    }
  )

  def preview(domain: String = "easiersolar") = actionBuilder.SubjectPresentAction().defaultHandler()(
    withSessionIDAndCurrentEventID {
      (session, event) => {
        implicit request =>
          val pageType = PageType.Preview.toString
          val Settings(landingSettings,siteSettings) = TemplateSettings.get_settings(domain=domain,directory="preview")
          Ok(views.html.angular.index(landingSettings, siteSettings, None))
      }
    }
  )

  def flush = Action.async { implicit request =>
    Future.successful(TemplateSettings.flush)
  }

  def assign_session = Action.async( withSessionIDAndNewEventID{(session,event) => {implicit request => Ok("")}})
}