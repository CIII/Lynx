package controllers

import javax.inject.Inject
import javax.inject.Provider

import play.Application
import play.api.mvc.{Action, Controller}
import utils.utilities._

import scala.concurrent.{Await, Future}
import com.redis._
import play.api.libs.json._
import dao.{PowerProvidersDAO, ZipApiDAO}
import play.api.libs.json.JsValue
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import play.Logger

/** Provides endpoints for interacting with the power suppliers collection, which is stored in AWS Redshift (thus the
 *  'Redis' reference).
 */
class RedisController @Inject()(
    appProvider: Provider[Application],
    providers: PowerProvidersDAO,
    zipApiDAO: ZipApiDAO
    ) extends Controller{

  def get_power_suppliers(state: String) = Action.async { implicit request =>
    var suppliersList: Option[List[Option[String]]]= Option(null)
    suppliersList = providers.getProviders(state)
    if (suppliersList.isDefined) suppliersList = Option(suppliersList.get.sorted)
    
    val suppliersListJson = Json.toJson(suppliersList)
    
    Future.successful(Ok(suppliersListJson))
  }

  def best_guess_supplier(zip: String) = Action.async { implicit request =>
    providers.bestGuessZipProvider(zip) match {
      case Some(powerCompany) =>
        Future.successful(Ok(Json.toJson(Map("supplier" -> powerCompany))))
      case None =>
        //May be in db
        zipApiDAO.findByZip(zip) map {
          case Some(zipApi) =>
            val powerCompany = zipApi.power_company
            providers.cacheBestGuessZipProvider(zip, powerCompany)
            Ok(Json.toJson(Map("supplier" -> powerCompany)))
          case None =>
            //Not in db
            NotFound
        }
    }

  }
}
