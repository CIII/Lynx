package api.solar.calculator.dao

import javax.inject.Inject

import api.solar.calculator.models.CostByZip
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class CostByZipDAO @Inject()(@NamedDatabase("solarcalculator") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val CostByZips = TableQuery[CostByZipTable]

  def all(): Future[Seq[CostByZip]] = db.run(CostByZips.result)

  def insert(cost_by_zip: CostByZip): Future[CostByZip] = db.run(CostByZips returning CostByZips.map(_.zip_code) into ((e, zip_code) => e.copy(zip_code = zip_code)) += cost_by_zip)

  def batchInsert(utilities: Seq[CostByZip]): Future[Unit] = db.run(CostByZips ++= utilities).map { _ => () }

  def update(cost_by_zip: CostByZip): Future[Unit] = {
    val formToUpdate = cost_by_zip.copy(cost_by_zip.zip_code)
    db.run(CostByZips.filter(_.zip_code === cost_by_zip.zip_code).update(formToUpdate)).map(_ => ())
  }

  def delete(zip_code: Int): Future[Unit] = db.run(CostByZips.filter(_.zip_code === zip_code).delete).map(_ => ())

  def find(zip_code: Int): Future[Option[CostByZip]] = {
    db.run(CostByZips.filter(_.zip_code === zip_code).result.headOption)
  }

  def find(zip_code: String): Future[Option[CostByZip]] = {
    val pattern = """0*(\d+)""".r
    pattern.findFirstIn(zip_code) match {
      case Some(zip_code) =>
        db.run (CostByZips.filter (_.zip_code === zip_code.toInt).result.headOption)
      case None =>
        Future(None)
    }
  }

  private class CostByZipTable(tag: Tag) extends Table[CostByZip](tag, "cost_by_zips") {
    def zip_code = column[Int]("zip_code", O.PrimaryKey, O.AutoInc)
    def cost = column[Float]("cost")
    def state = column[String]("state")

    private type CostByZipTupleType = (
      Int,
      Float,
      String
      )
    //
    private val formShapedValue = (zip_code, cost, state).shaped[CostByZipTupleType]
    //
    private val toModel: CostByZipTupleType => CostByZip = { solarYieldTuple =>
      CostByZip(
        zip_code = solarYieldTuple._1,
        cost = solarYieldTuple._2,
        state = solarYieldTuple._3
      )
    }
    private val toTuple: CostByZip => Option[CostByZipTupleType] = { solarYield =>
      Some {
        (
          solarYield.zip_code,
          solarYield.cost,
          solarYield.state
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}