package controllers.config_ui

import java.io.{File, FileWriter}

import akka.actor.ActorSystem
import api.SessionEvent
import api.SessionEvent._
import be.objectify.deadbolt.scala.ActionBuilders
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.cloudfront.model._
import com.amazonaws.services.cloudfront.{AmazonCloudFront, AmazonCloudFrontClientBuilder}
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model.{GetObjectRequest, PutObjectRequest}
import com.google.inject.Inject
import com.typesafe.config._
import org.apache.commons.io.FilenameUtils
import play.Logger
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Security}
import play.api.libs.json._
import play.twirl.api.Html
import security.dao.UserAccountDAO
import security.models.UserAccount
import utils.templates.{Settings, TemplateSettings}

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import play.api.Environment

/**
  * Created by slin on 9/3/17.
  */
class ConfigurationUIController @Inject()(
  userAccountDAO: UserAccountDAO,
  actionBuilder: ActionBuilders,
  system: ActorSystem,
  val messagesApi: MessagesApi,
  implicit val configuration: Configuration,
  implicit val environment: Environment
) extends Controller with I18nSupport {

  def login = Action.async {
    implicit request =>
      Logger.debug(request.session.get("redirect").toString)
      request.session.get(Security.username) match {
        case Some(userName) =>{
          val redirectURL = request.session.get("redirect") match {
            case Some(rURL) => rURL
            case _ => "/pages/admin"
          }
          Future(Redirect(redirectURL))
        }
        case None => Future(Ok(views.html.conf_ui.login(loginForm)))
      }
  }

  def login_error = Action.async {
    implicit request =>
      Future.successful(Ok(views.html.conf_ui.login_error()))
  }

  def authenticate = Action.async {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.conf_ui.login(formWithErrors))),
        user => {
          val redirectURL = request.session.get("redirect") match {
            case Some(rURL) => rURL
            case _ => "/pages/admin"
          }
          Future.successful(Redirect(redirectURL).withSession(request.session + (Security.username -> user.get.username)))
        }
      )
  }

  private def db_authenticate(user_name: String, password: String): Option[UserAccount] = {
    Await.result(userAccountDAO.findByUserNameAndPassword(user_name,password), Duration.Inf)
  }

  val loginForm = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(db_authenticate)(_.map(u => (u.username, ""))).verifying("Invalid username or password.", result => result.isDefined)
  )

  def index = actionBuilder.SubjectPresentAction().defaultHandler(){
    implicit request =>
      Future.successful(Ok(views.html.conf_ui.index()))
  }

  def view(view: String) = Action.async {
    request =>
      val viewPattern = """(.+).html""".r
      Logger.debug(view)
      val viewResult = view match {
        case viewPattern(viewCode) =>
          try {
            Logger.debug(viewCode)
            val clazz: Class[_] = Class.forName(s"views.html.conf_ui.${viewCode}");
            val render: java.lang.reflect.Method = clazz.getDeclaredMethod("render", configuration.getClass);
            Ok(render.invoke(null, configuration).asInstanceOf[Html])
          } catch {
            case e: Throwable =>
              play.api.Logger.error(s"[ConfigUI] This is a view that does not exist yet, ${view}")
              NotFound("This is a view that does not exist yet")
          }
        case _ =>
          play.api.Logger.error(s"[ConfigUI] This is a view that does not exist yet, ${view}")
          NotFound("This is a view that does not exist yet")
      }

      Future.successful(viewResult)
  }

  def current_domain_config = Action.async{
    implicit request =>

      val currentConfig = utils.templates.TemplateSettings.TEMPLATE_CONFIGURATION.underlying.root().render(
        ConfigRenderOptions.concise())

      Future.successful(Ok(currentConfig))
  }

  def upload_domain_config = actionBuilder.SubjectPresentAction().defaultHandler(){
    implicit request =>
      try {
        val newConfigRaw = request.body.asJson.get.toString
        val newConfig = ConfigFactory.parseString(newConfigRaw, ConfigParseOptions.defaults()
          .setSyntax(ConfigSyntax.JSON)).resolve()
        val newConfigStr = newConfig.root().render(ConfigRenderOptions.concise().setFormatted(true))
        val uploadResult = uploadConfigToS3(newConfigStr)
        Future.successful(Ok(s"Uploading new config: \n${uploadResult.toString}"))
      }catch{
        case e: Throwable =>
          e.printStackTrace()
          Future.successful(BadRequest(s"unable to parse submitted config ${request.body.toString}"))
      }
  }

  def current_ab_tests = Action.async{
    implicit request =>
      val currentABTests = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, String]]()
      val abTestDir = "abTest"
      val abTestYamlDir = s"${abTestDir}/testYaml"
      val abTestJsonDir = s"${abTestDir}/testJson"

      (new File(abTestYamlDir)).listFiles.map {
        file =>
          val fileName = file.getName.replace(".yml", "")
          val source = scala.io.Source.fromFile(file)
          val lines = try source.mkString finally source.close()

          currentABTests += fileName -> scala.collection.mutable.Map[String,String]("yaml" -> lines)
      }

      (new File(abTestJsonDir)).listFiles.map {
        file =>
          val fileName = file.getName.replace(".json", "")
          val source = scala.io.Source.fromFile(file)
          val lines = try source.mkString finally source.close()

          currentABTests(fileName) += "json" -> lines
      }

        Future.successful(Ok(Json.toJson(currentABTests)))
  }

  def upload_ab_tests = Action.async{
    implicit request =>

      try {
        val accessKey = configuration.getString("s3.access.key")
        val secretKey = configuration.getString("s3.secret.key")
        val bucketName = configuration.getString("s3.bucket.name")

        val abTestDir = "ab_tests"

        (accessKey, secretKey, bucketName) match {
          case (Some(aKey), Some(sKey), Some(bucket)) =>
            request.body.asJson match {
              case Some(experimentJson) =>
                try {

                  val creds: BasicAWSCredentials = new BasicAWSCredentials(aKey, sKey);
                  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("us-west-2").build();

                  experimentJson.as[List[JsObject]].foreach {
                    experimentJsObj =>

                      val experimentName = (experimentJsObj \ "name").as[String]
                      val experimentYaml = (experimentJsObj \ "yaml").as[String]

                      val tempYamlFile = s"${experimentName}.yml.tmp"
                      (new File(tempYamlFile)).delete()
                      val fw = new FileWriter(tempYamlFile, true)
                        fw.write(experimentYaml)
                        fw.close()
                        val key = s"$abTestDir/${experimentName}.yml"
                        val file = new File(tempYamlFile)
                        s3Client.putObject(new PutObjectRequest(bucket, key, file))
                        file.delete()
                  }
                } catch {
                  case e: Throwable =>
                    Logger.error(s"[ConfigUIController] Cannot upload passed yaml ${e.getMessage}")
                    Future.successful(BadRequest("Cannot upload"))
                }
                Future.successful(Ok("Yamls updated"))
              case _ =>
                Future.successful(BadRequest("Cannot upload, no expeiments"))
            }
          case _ =>
            Future.successful(BadRequest("No access key or secret key for s3 in environment"))
        }
      }catch {
        case e: Throwable =>
          e.printStackTrace()
          Future.successful(BadRequest("Cannot upload new experiments"))
      }

  }

  private def uploadConfigToS3(config: String)(implicit configuration: Configuration): List[String] =
    try {
      val accessKey = configuration.getString("s3.access.key")
      val secretKey = configuration.getString("s3.secret.key")
      val bucketName = configuration.getString("s3.bucket.name")
      (accessKey, secretKey, bucketName) match {
        case (Some(aKey), Some(sKey), Some(bucket)) =>
          val tempTemplateFile = s"${utils.templates.TemplateSettings.TEMPLATE_SETTINGS_DIR}/${utils.templates.TemplateSettings.TEMPLATE_CONF}.tmp"

          val creds: BasicAWSCredentials = new BasicAWSCredentials(aKey, sKey);
          val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("us-west-2").build();

          //Write config to temporary file
          (new File(tempTemplateFile)).delete()
          val fw = new FileWriter(tempTemplateFile, true)
          try {
            fw.write(config)
            fw.close()
            try {
              //Upload file to s3
              val key = s"${utils.templates.TemplateSettings.TEMPLATE_SETTINGS_DIR}/${utils.templates.TemplateSettings.TEMPLATE_CONF}"
              val file = new File(tempTemplateFile)
              s3Client.putObject(new PutObjectRequest(bucket, key, file))
              file.delete()
              Logger.debug(s"[Template Settings] $key downloaded")
            }catch{
              case e: Exception =>
                e.printStackTrace()
                return List[String](s"Could not upload configuration file: ${e.toString}")
            }
          } catch {
            case e: Throwable =>
              e.printStackTrace()
              fw.close()
              return List[String](s"Unable to create temporary file: ${e.toString}")
          }

          return List[String]("New configurations uploaded")
        case _ =>
          return List[String]("No access key or secret key for s3 in environment")
      }

    } catch {
      case e: Exception => {
        Logger.error("[Template Configs] Could not flush new configs")
        Logger.debug(e.toString)
        List[String]("Could not flush new configs", e.toString)
      }
    }

  def create_invalidation = actionBuilder.SubjectPresentAction().defaultHandler(){
    implicit request =>
      val accessKey = configuration.getString("s3.access.key")
      val secretKey = configuration.getString("s3.secret.key")
      val bucketName = configuration.getString("s3.bucket.name")

      try {
        (accessKey, secretKey, bucketName) match {
          case (Some(aKey), Some(sKey), Some(bucket)) =>
            configuration.getString("cloudfront.cdn.distribution_id") match {
              case Some(distribution_id) =>
                val creds: BasicAWSCredentials = new BasicAWSCredentials(aKey, sKey);
                val cloudfrontClient: AmazonCloudFront = AmazonCloudFrontClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("us-west-2").build();
                //Declaring paths of items to invalidate
                import collection.JavaConversions._
                val paths: Paths = new Paths
                val keys = request.queryString.get("paths") match {
                  case Some(sequence) => sequence
                  case _ => Seq("/easiersolar/static/*", "/homesolar/static/*")
                }
                paths.setItems(keys)
                paths.setQuantity(keys.size)
                //Create request
                val invalidationBatch: InvalidationBatch = new InvalidationBatch(paths, utils.utilities.cache_breaker.get)
                val invalidationRequest: CreateInvalidationRequest = new CreateInvalidationRequest(distribution_id,
                  invalidationBatch)
                //Submit request
                val invalidationId = cloudfrontClient.createInvalidation(invalidationRequest).getInvalidation.getId
                Future.successful(Ok("{\"invalidation\":{\"id\":\"" + invalidationId + "\"}}"))
              case _ =>
                Future.successful(BadRequest("No cdn distribution_id, dev environment"))
            }
          case _ =>
            Future.successful(BadRequest("No access key or secret key for cloudfront in environment"))
        }
      }catch{
        case e: Throwable =>
          e.printStackTrace()
          Future.successful(BadRequest(s"Unable to create invalidation: ${e.toString}"))
      }

  }
  private def isInvalidationComplete(invalidationId: String): Boolean = {
    val accessKey = configuration.getString("s3.access.key")
    val secretKey = configuration.getString("s3.secret.key")
    val bucketName = configuration.getString("s3.bucket.name")
    val distributionId = configuration.getString("cloudfront.cdn.distribution_id")

    (accessKey, secretKey, bucketName, distributionId) match {
      case(Some(aKey), Some(sKey), Some(bucket), Some(dID)) =>
        try {
          val creds: BasicAWSCredentials = new BasicAWSCredentials(aKey, sKey);
          val cloudfrontClient: AmazonCloudFront = AmazonCloudFrontClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("us-west-2").build();

          val getInvalidationRequest = new GetInvalidationRequest(dID, invalidationId)
          cloudfrontClient.getInvalidation(getInvalidationRequest).getInvalidation.getStatus.toLowerCase == "completed"
        }catch{
          case ni: NoSuchInvalidationException =>
            Logger.debug(s"[ConfigurationUIController] No such invalidation ${invalidationId}")
            false
          case e: Throwable =>
            Logger.error(s"[ConfigurationUIController] Error checking invalidation: ${e.getMessage}")
            false
        }
      case _ =>
        false
    }
   }
  def check_invalidations = actionBuilder.SubjectPresentAction().defaultHandler(){
    implicit request =>
      var invalidationResults = Map[String,Boolean]()
      request.queryString.get("invalidations").getOrElse(Seq()).foreach(
        invalidationId =>
          invalidationResults += (invalidationId -> isInvalidationComplete(invalidationId))
      )
      Future.successful(Ok(Json.stringify(Json.toJson(invalidationResults))))
  }

  def list_available_images = actionBuilder.SubjectPresentAction().defaultHandler() {
    implicit request =>

      val accessKey = configuration.getString("s3.access.key")
      val secretKey = configuration.getString("s3.secret.key")
      val bucketName = configuration.getString("s3.bucket.name")

      (accessKey, secretKey, bucketName) match {
        case (Some(aKey), Some(sKey), Some(bucket)) =>
          try{
            val creds: BasicAWSCredentials = new BasicAWSCredentials(aKey, sKey);
            val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("us-west-2").build();
            val availableImages = new ListBuffer[String]()
            import scala.collection.JavaConversions._
            var objectsList = s3Client.listObjects(bucket)

            for(summary <- objectsList.getObjectSummaries()) {
              val key: String = summary.getKey
              Logger.debug(key)
              if (key.contains("images/")) {
                availableImages += key
              }
            }

            while(objectsList.isTruncated){
              objectsList = s3Client.listNextBatchOfObjects(objectsList)
              for(summary <- objectsList.getObjectSummaries()) {
                val key: String = summary.getKey
                Logger.debug(key)
                if (key.contains("images/")) {
                  availableImages += key
                }
              }
            }
            Future.successful(Ok(Json.stringify(Json.toJson(availableImages.toList))))
          }catch{
            case e: Throwable =>
              Logger.error(s"[ConfigurationUIController] Error listing available images: ${e.getMessage}")
              Future.successful(BadRequest("Error listing available images"))
          }
        case _ =>
          Logger.error(s"[ConfigurationUIController] Error listing available images:\n\thas access-key ${!accessKey.isEmpty}\n\thas secret-key ${!secretKey.isEmpty}\n\thas bucket-name ${!bucketName.isEmpty}")
          Future.successful(BadRequest("Error listing available images"))
      }
  }

}
