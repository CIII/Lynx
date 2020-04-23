package utils.templates

import java.io.{File, FileOutputStream}
import java.util
import java.util.Properties

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FilenameUtils
import play.Logger
import play.api.{Configuration, Play}

import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.List
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import org.joda.time.DateTime
import play.api.mvc.{Result, Results}

sealed class TemplateSettings

case class GetQuotesSettings(page_headers: List[String], hero_image_path: String) extends TemplateSettings

case class NavBarSettings(form_page: String) extends TemplateSettings

case class Settings(homeSettings: LandingSettings, siteSettings: SiteSettings)

case class LandingSettings(fullForm: Boolean, showAddressBar: Boolean, defaultHeader: String, var targetCity: Option[String],
                           var targetState: Option[String], var targetBrand: Option[String], ctaText: String, heroImagePath: String,
                           sectionTwoSettings: SectionTwoSettings) extends TemplateSettings

case class SectionTwoSettings(defaultHeader: String, bullets: java.util.List[String], aboveCTA: String, ctaText: String, compareImagePath: String) extends TemplateSettings

case class SiteSettings(siteName: String, siteTitle: String, pageDescription: String, faviconPath: String, logoPath: String, footerLogoPath: String,
                        activeHomeSections: java.util.List[Integer], cssPath: String, gtmContainer: String,
                        fb_app_id: String, showSolarCalc: Boolean, googleConversionId: String,
                        googleConversionLabel: String, capitalSiteName: String, host: String,
                        fullFormSequence: List[Integer], shortFormSequence: List[Integer], directory: String,
                        loadRules: String, submitRules: String, socialShareImage: String
                       ) extends TemplateSettings

case class HeaderSettings(externalPage: Boolean) extends TemplateSettings

object TemplateSettings {

  val TEMPLATE_SETTINGS_DIR: String = "templates_conf"
  val TEMPLATE_CONF: String = "domains.conf"
  var TEMPLATE_CONFIGURATION: Configuration = _

  var EASIERSOLAR_HOMEPAGE_SETTINGS: Settings = null;
  var EASIERSOLAR_GETQUOTES_SETTINGS: Settings = null;
  var EASIERSOLAR_CITY_SETTINGS: Settings = null;
  var EASIERSOLAR_SOLARCITYDEALS_SETTINGS: Settings = null;
  var EASIERSOLAR_SOLARREBATES_SETTINGS: Settings = null;
  var EASIERSOLAR_FINANCING_SETTINGS: Settings = null;
  var EASIERSOLAR_PREVIEW_SETTINGS: Settings = null;
  var HOMESOLAR_HOMEPAGE_SETTINGS: Settings = null;
  var HOMESOLAR_GETQUOTES_SETTINGS: Settings = null;
  var HOMESOLAR_BRAND_SETTINGS: Settings = null;
  var HOMESOLAR_ELECTRICITY_SAVINGS_SETTINGS: Settings = null;
  var HOMESOLAR_PREVIEW_SETTINGS: Settings = null;
  var MUTUALOMAHA_HOMEPAGE_SETTINGS: Settings = null;

  def construct_setting(domain: String, directory: String): Settings = {
    def listStringToList(listString: String):Array[String] = {
      listString.split(",")
    }
    val fullForm = TEMPLATE_CONFIGURATION.getBoolean("lynx.domains." + domain + "." + directory + ".full_form").getOrElse(TEMPLATE_CONFIGURATION.getBoolean("lynx.domains." + domain + ".home_page.full_form").get)
    val showAddressBar = TEMPLATE_CONFIGURATION.getBoolean("lynx.domains." + domain + "." + directory + ".show_address_bar").getOrElse(TEMPLATE_CONFIGURATION.getBoolean("lynx.domains." + domain + ".home_page.show_address_bar").get)
    val defaultHeader = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".default_header").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.default_header").get)
    val targetCity = None
    val targetState = None
    val targetBrand = None
    val ctaText = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".cta").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.cta").get)
    val heroImagePath = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".hero_image_path").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.hero_image_path").get)
    val defaultHeader2 = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".home_section_two_header").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.home_section_two_header").get)
    val bullets = TEMPLATE_CONFIGURATION.getStringList("lynx.domains." + domain + "." + directory + ".home_section_two_bullets").getOrElse(TEMPLATE_CONFIGURATION.getStringList("lynx.domains." + domain + ".home_page.home_section_two_bullets").get)
    val aboveCTA = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".home_section_two_above_cta").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.home_section_two_above_cta").get)
    val ctaText2 = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".home_section_two_cta").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.home_section_two_cta").get)
    val compareImagePath = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".compare_image_path").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.compare_image_path").get)
    val siteTitle = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".site_title").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.site_title").get)
    val pageDescription = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".page_description").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.page_description").get)
    val faviconPath = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".favicon_path").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.favicon_path").get)
    val logoPath = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".logo").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.logo").get)
    val footerLogoPath = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".footer_logo").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.footer_logo").get)
    val activeHomeSections = TEMPLATE_CONFIGURATION.getIntList("lynx.domains." + domain + "." + directory + ".active_home_sections").getOrElse(TEMPLATE_CONFIGURATION.getIntList("lynx.domains." + domain + ".home_page.active_home_sections").get)
    val cssPath = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".css").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.css").get)
    val gtmContainer = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".gtm_container").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.gtm_container").get)
    val fbAppId = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".facebook_app_id").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.facebook_app_id").get)
    val showSolarCalc = TEMPLATE_CONFIGURATION.getBoolean("lynx.domains." + domain + "." + directory + ".show_solar_calc").getOrElse(TEMPLATE_CONFIGURATION.getBoolean("lynx.domains." + domain + ".home_page.show_solar_calc").get)
    val googleConversionId = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".google_conversion_id").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.google_conversion_id").get)
    val googleConversionLabel = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".google_conversion_label").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.google_conversion_label").get)
    val capitalSiteName = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".capital_domain").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.capital_domain").get)
    val host = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".host").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.host").get)
    val fullFormSequence = TEMPLATE_CONFIGURATION.getIntList("lynx.domains." + domain + "." + directory + ".full_form_sequence").getOrElse(TEMPLATE_CONFIGURATION.getIntList("lynx.domains." + domain + ".home_page.full_form_sequence").get).toList
    val shortFormSequence = TEMPLATE_CONFIGURATION.getIntList("lynx.domains." + domain + "." + directory + ".short_form_sequence").getOrElse(TEMPLATE_CONFIGURATION.getIntList("lynx.domains." + domain + ".home_page.short_form_sequence").get).toList
    val submitRules = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".submit_rules").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.submit_rules").get)
    val loadRules = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".load_rules").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.load_rules").get)
    val socialShareImage = TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + "." + directory + ".social_share_image").getOrElse(TEMPLATE_CONFIGURATION.getString("lynx.domains." + domain + ".home_page.social_share_image").get)
    Settings(
      homeSettings = LandingSettings(fullForm = fullForm,
        showAddressBar = showAddressBar,
        defaultHeader = defaultHeader,
        targetCity = targetCity, targetState = targetState, targetBrand = targetBrand, ctaText = ctaText,
        heroImagePath = heroImagePath,
        sectionTwoSettings = SectionTwoSettings(
          defaultHeader = defaultHeader2,
          bullets = bullets,
          aboveCTA = aboveCTA,
          ctaText = ctaText2,
          compareImagePath = compareImagePath

        )),
      siteSettings =
        SiteSettings(siteName = domain,
          siteTitle = siteTitle,
          pageDescription = pageDescription,
          directory = directory,
          faviconPath = faviconPath,
          logoPath = logoPath,
          footerLogoPath = footerLogoPath,
          activeHomeSections = activeHomeSections,
          cssPath = cssPath,
          gtmContainer = gtmContainer,
          fb_app_id = fbAppId,
          showSolarCalc = showSolarCalc,
          googleConversionId = googleConversionId,
          googleConversionLabel = googleConversionLabel,
          capitalSiteName = capitalSiteName,
          host = host,
          fullFormSequence = fullFormSequence,
          shortFormSequence = shortFormSequence,
          submitRules = submitRules,
          loadRules = loadRules,
          socialShareImage = socialShareImage
        )
    )
  }

  def get_settings(domain: String = "easiersolar", directory: String = "home_page"): Settings = {
    domain match {
      case "easiersolar" =>
        directory match {
          case "home_page" => EASIERSOLAR_HOMEPAGE_SETTINGS
          case "get_quotes" => EASIERSOLAR_GETQUOTES_SETTINGS
          case "city" => EASIERSOLAR_CITY_SETTINGS
          case "solar_city_deals" => EASIERSOLAR_SOLARCITYDEALS_SETTINGS
          case "solar_rebates" => EASIERSOLAR_SOLARREBATES_SETTINGS
          case "financing" => EASIERSOLAR_FINANCING_SETTINGS
          case "preview" => EASIERSOLAR_PREVIEW_SETTINGS
          case _ => EASIERSOLAR_HOMEPAGE_SETTINGS
        }
      case "homesolar" =>
        directory match {
          case "home_page" => HOMESOLAR_HOMEPAGE_SETTINGS
          case "get_quotes" => HOMESOLAR_GETQUOTES_SETTINGS
          case "brand" => HOMESOLAR_BRAND_SETTINGS
          case "electricity_savings" => HOMESOLAR_ELECTRICITY_SAVINGS_SETTINGS
          case "preview" => HOMESOLAR_PREVIEW_SETTINGS
          case _ => HOMESOLAR_HOMEPAGE_SETTINGS
        }
      case "mutualofomahamedicareplans" =>
        MUTUALOMAHA_HOMEPAGE_SETTINGS
      case _ =>
        EASIERSOLAR_HOMEPAGE_SETTINGS
    }
  }

  def get_hero_image_path(ts: TemplateSettings): String = ts match {
    case GetQuotesSettings(_, image_path) => image_path
    case LandingSettings(_,_,_,_,_,_,_,heroImagePath,_) => heroImagePath
    case _ => throw new InvalidTemplateException()
  }

  def get_header_insert(ts: TemplateSettings): String = ts match {
    case GetQuotesSettings(page_headers: List[String], _) => {
      var header = ""
      for (a <- 0 to page_headers.length - 2) {
        header += page_headers(a) + "<br>"
      }
      header + page_headers.last
    }
    case _ => throw new InvalidTemplateException()
  }

  def get_form_page_redirect(ts: TemplateSettings): String = ts match {
    case NavBarSettings(form_page) => form_page
    case _ => throw new InvalidTemplateException()
  }

  def is_full_landing(ts: TemplateSettings): Boolean = ts match {
    case LandingSettings(fullForm,_,_,_,_,_,_,_,_) => fullForm
    case _ => throw new InvalidTemplateException()
  }

  def show_address_bar(ts: TemplateSettings): Boolean = ts match {
    case LandingSettings(_,showAddressBar,_,_,_,_,_,_,_) => showAddressBar
    case _ => throw new InvalidTemplateException()
  }
  def get_default_header(ts: TemplateSettings): String = ts match {
    case LandingSettings(_,_,defaultHeader,_,_,_,_,_,_) => defaultHeader
    case SectionTwoSettings(defaultHeader,_,_,_,_) => defaultHeader
    case _ => throw new InvalidTemplateException()
  }

  def get_bullets(ts: TemplateSettings): java.util.List[String] = ts match {
    case SectionTwoSettings(_,bullets,_,_,_) => bullets
    case _ => throw new InvalidTemplateException()
  }

  def get_target_city(ts: TemplateSettings): Option[String] = ts match {
    case LandingSettings(_,_,_,targetCity,_,_,_,_,_) => targetCity
    case _ => throw new InvalidTemplateException()
  }

  def get_target_brand(ts: TemplateSettings): Option[String] = ts match {
    case LandingSettings(_,_,_,_,_,targetBrand,_,_,_) => targetBrand
    case _ => throw new InvalidTemplateException()
  }

  def get_target_state(ts: TemplateSettings): Option[String] = ts match {
    case LandingSettings(_,_,_,_,targetState,_,_,_,_) => targetState
    case _ => throw new InvalidTemplateException()
  }

  def get_above_cta_text(ts: TemplateSettings): String = ts match {
    case SectionTwoSettings(_,_,aboveCTA,_,_) => aboveCTA
    case _ => throw new InvalidTemplateException()
  }

  def get_cta_text(ts: TemplateSettings): String = ts match {
    case LandingSettings(_,_,_,_,_,_,ctaText,_,_) => ctaText
    case SectionTwoSettings(_,_,_,ctaText,_) => ctaText
    case _ => throw new InvalidTemplateException()
  }

  def is_external_page(ts: TemplateSettings): Boolean = ts match {
    case HeaderSettings(externalPage) => externalPage
    case _ => throw new InvalidTemplateException()
  }

  def get_site_name(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.siteName
    case _ => throw new InvalidTemplateException()
  }

  def get_favicon_path(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.faviconPath
    case _ => throw new InvalidTemplateException()
  }

  def get_site_title(ts: TemplateSettings, ls: LandingSettings): String = ts match {
    case ss: SiteSettings => ss.siteTitle.replace("%target_city", ls.targetCity.getOrElse("")).replace("%target_brand", ls.targetBrand.getOrElse(""))
    case _ => throw new InvalidTemplateException()
  }
  
  def get_page_description(ts: TemplateSettings, ls: LandingSettings): String = ts match {
    case ss: SiteSettings => ss.pageDescription.replace("%target_city", ls.targetCity.getOrElse("")).replace("%target_brand", ls.targetBrand.getOrElse(""))
    case _ => throw new InvalidTemplateException()
  }

  def get_logo_path(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.logoPath
    case _ => throw new InvalidTemplateException()
  }

  def get_footer_logo_path(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.footerLogoPath
    case _ => throw new InvalidTemplateException()
  }

  def get_active_home_sections(ts: TemplateSettings): java.util.List[Integer] = ts match {
    case ss: SiteSettings => ss.activeHomeSections
    case _ => throw new InvalidTemplateException()
  }

  def get_css_path(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.cssPath
    case _ => throw new InvalidTemplateException()
  }

  def get_gtm_container(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.gtmContainer
    case _ => throw new InvalidTemplateException()
  }

  def get_fb_app_id(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.fb_app_id
    case _ => throw new InvalidTemplateException()
  }

  def show_solar_cal(ts: TemplateSettings): Boolean = ts match {
    case ss: SiteSettings => ss.showSolarCalc
    case _ => throw new InvalidTemplateException()
  }

  def get_google_conversion_id(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.googleConversionId
    case _ => throw new InvalidTemplateException()
  }

  def get_google_conversion_label(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.googleConversionLabel
    case _ => throw new InvalidTemplateException()
  }

  def get_capital_site_name(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.capitalSiteName
    case _ => throw new InvalidTemplateException()
  }

  def get_host(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.host
    case _ => throw new InvalidTemplateException()
  }

  def get_compare_image_path(ts: TemplateSettings): String = ts match {
    case SectionTwoSettings(_,_,_,_,compareImagePath) => compareImagePath
    case _ => throw new InvalidTemplateException()
  }

  def get_full_form_sequence(ts: TemplateSettings): List[Integer] = ts match {
    case ss: SiteSettings => ss.fullFormSequence
    case _ => throw new InvalidTemplateException()
  }

  def get_short_form_sequence(ts: TemplateSettings): List[Integer] = ts match {
    case ss: SiteSettings => ss.shortFormSequence
    case _ => throw new InvalidTemplateException()
  }

  def get_directory(ts: TemplateSettings): String = ts match {
    case ss: SiteSettings => ss.directory
    case _ => throw new InvalidTemplateException()
  }
  
  def get_load_rules(ts: TemplateSettings): String = ts match{
    case ss: SiteSettings => ss.loadRules
    case _ => throw new InvalidTemplateException()
  }
  
  def get_submit_rules(ts: TemplateSettings): String = ts match{
    case ss: SiteSettings => ss.submitRules
    case _ => throw new InvalidTemplateException()
  }
  
  def get_social_share_image(ts: TemplateSettings): String = ts match{
    case ss: SiteSettings => ss.socialShareImage
    case _ => throw new InvalidTemplateException()
  }


  def createTemplatesConfDir = {
    val templatesConfDir: File = new File(TEMPLATE_SETTINGS_DIR);

      templatesConfDir.mkdirs();
  }

  def construct_settings() = {
    val key = s"${TEMPLATE_SETTINGS_DIR}/$TEMPLATE_CONF"
    Logger.debug(s"Key: $key")
    TEMPLATE_CONFIGURATION = Configuration(ConfigFactory.parseFile(new File(key)).resolve())
    Logger.debug("[Template Settings] Loading template settings")
    EASIERSOLAR_HOMEPAGE_SETTINGS = construct_setting("easiersolar","home_page")
    EASIERSOLAR_GETQUOTES_SETTINGS = construct_setting("easiersolar","get_quotes")
    EASIERSOLAR_CITY_SETTINGS = construct_setting("easiersolar","city")
    EASIERSOLAR_SOLARCITYDEALS_SETTINGS = construct_setting("easiersolar","solar_city_deals")
    EASIERSOLAR_SOLARREBATES_SETTINGS = construct_setting("easiersolar","solar_rebates")
    EASIERSOLAR_FINANCING_SETTINGS = construct_setting("easiersolar","financing")
    EASIERSOLAR_PREVIEW_SETTINGS = construct_setting("easiersolar","preview")
    HOMESOLAR_HOMEPAGE_SETTINGS = construct_setting("homesolar","home_page")
    HOMESOLAR_GETQUOTES_SETTINGS = construct_setting("homesolar","get_quotes")
    HOMESOLAR_BRAND_SETTINGS = construct_setting("homesolar","brand")
    HOMESOLAR_ELECTRICITY_SAVINGS_SETTINGS = construct_setting("homesolar","electricity_savings")
    HOMESOLAR_PREVIEW_SETTINGS = construct_setting("homesolar","preview")
    MUTUALOMAHA_HOMEPAGE_SETTINGS = construct_setting("mutualofomahamedicareplans", "home_page")
  }

  private def updateCacheBreaker() = {
    //Update values
    //TODO Write to properties file
    val newCacheBreaker = (new DateTime).toString("yyyyMMddHHmmss")
    utils.utilities.cache_breaker = Some(newCacheBreaker)
  }

  def flush(implicit configuration: Configuration): Result =
  try {
    createTemplatesConfDir
    val accessKey = configuration.getString("s3.access.key")
    val secretKey = configuration.getString("s3.secret.key")
    val bucketName = configuration.getString("s3.bucket.name")
    (accessKey, secretKey, bucketName) match {
      case (Some(aKey), Some(sKey), Some(bucket)) =>
        val creds: BasicAWSCredentials = new BasicAWSCredentials(aKey, sKey);
        val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("us-west-2").build();
        try {
          val key = s"${TEMPLATE_SETTINGS_DIR}/$TEMPLATE_CONF"
          s3Client.getObject(new GetObjectRequest(bucket, key), new File(key))
          Logger.debug(s"[Template Settings] $key downloaded")
        }catch{
          case e: Exception =>
            e.printStackTrace()
            return Results.BadRequest(s"Could not download configuration file: ${e.toString}")
        }

        construct_settings()
        updateCacheBreaker()

        Results.Ok("New configurations loaded")
      case _ =>
        Results.BadRequest("No access key or secret key for s3 in environment")
    }
  } catch {
      case e: Exception =>
          Logger.error("[Template Configs] Could not flush new configs")
          Logger.debug(e.toString)
          Results.BadRequest(s"Could not flush new configs ${e.toString}")
  }

  def domain_host(domain: String): String = {
    domain match {
      case "easiersolar" =>
          EASIERSOLAR_HOMEPAGE_SETTINGS.siteSettings.host
      case "homesolar" =>
          HOMESOLAR_HOMEPAGE_SETTINGS.siteSettings.host
      case _ =>
          EASIERSOLAR_HOMEPAGE_SETTINGS.siteSettings.host
    }
  }
}

class InvalidTemplateException(message: Option[String] = None, cause: Option[Throwable] = None)
  extends RuntimeException(InvalidTemplateException.defaultMessage(message, cause), cause.orNull)

object InvalidTemplateException {
  def defaultMessage(message: Option[String], cause: Option[Throwable]) = message.getOrElse(cause.getOrElse("")).asInstanceOf[String]
}

@Singleton
class TemplatesStartup @Inject()(
  implicit val configuration: play.api.Configuration){
  if(configuration.getBoolean("domains.flush.on_startup").getOrElse(false)) {
    TemplateSettings.flush
  }else{
    TemplateSettings.construct_settings
  }
}
