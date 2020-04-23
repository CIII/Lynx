package dao

import org.joda.time.DateTime

import scala.concurrent.Future

import javax.inject.Inject
import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import com.github.tototoshi.slick.MySQLJodaSupport._

case class ZipApi(zip: String, power_company: String)

class ZipApiDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val ZipApis = TableQuery[ZipApiTable]

  def all(): Future[Seq[ZipApi]] = db.run(ZipApis.result)

  def insert(zipApi: ZipApi): Future[ZipApi] = db.run(ZipApis returning ZipApis.map(_.zip) into ((u, zip) => u.copy(zip = zip)) += zipApi)

  def batchInsert(zipApis: Seq[ZipApi]): Future[Unit] = db.run(ZipApis ++= zipApis).map { _ => () }

  def findByZip(zip: String): Future[Option[ZipApi]] = {
    db.run(ZipApis.filter(_.zip === zip).result.headOption)
  }

  private class ZipApiTable(tag: Tag) extends Table[ZipApi](tag, "zip_power_company_api") {
    def zip = column[String]("zip", O.PrimaryKey)
    def power_company = column[String]("power_company")

    private type ZipApiTupleType = (
        String,
        String
      )
    //
    private val formShapedValue = (zip, power_company).shaped[ZipApiTupleType]
    //
    private val toModel: ZipApiTupleType => ZipApi = { zipApiTuple =>
      ZipApi(
        zip = zipApiTuple._1,
        power_company = zipApiTuple._2
      )
    }
    private val toTuple: ZipApi => Option[ZipApiTupleType] = { zipApi =>
      Some {
        (
          zipApi.zip,
          zipApi.power_company
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}