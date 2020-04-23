package api.solar.calculator.dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import api.solar.calculator.models.SolarYield
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class SolarYieldDAO @Inject()(@NamedDatabase("solarcalculator") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val SolarYields = TableQuery[SolarYieldTable]

  def all(): Future[Seq[SolarYield]] = db.run(SolarYields.result)

  def insert(solar_yield: SolarYield): Future[SolarYield] = db.run(SolarYields returning SolarYields.map(_.zip_code) into ((e, zip_code) => e.copy(zip_code = zip_code)) += solar_yield)

  def batchInsert(utilities: Seq[SolarYield]): Future[Unit] = db.run(SolarYields ++= utilities).map { _ => () }

  def update(solar_yield: SolarYield): Future[Unit] = {
    val formToUpdate = solar_yield.copy(solar_yield.zip_code)
    db.run(SolarYields.filter(_.zip_code === solar_yield.zip_code).update(formToUpdate)).map(_ => ())
  }

  def delete(zip_code: Int): Future[Unit] = db.run(SolarYields.filter(_.zip_code === zip_code).delete).map(_ => ())

  def find(zip_code: Int): Future[Option[SolarYield]] = {
    db.run(SolarYields.filter(_.zip_code === zip_code).result.headOption)
  }

  def find(zip_code: String): Future[Option[SolarYield]] = {
    val pattern = """0*(\d+)""".r
    pattern.findFirstIn(zip_code) match {
      case Some(zip_code) =>
        db.run (SolarYields.filter (_.zip_code === zip_code.toInt).result.headOption)
      case None =>
        Future(None)
    }
  }

  private class SolarYieldTable(tag: Tag) extends Table[SolarYield](tag, "solar_yields") {
    def zip_code = column[Int]("zip_code", O.PrimaryKey, O.AutoInc)
    def ac_monthly_1 = column[Float]("ac_monthly_1")
    def ac_monthly_2 = column[Float]("ac_monthly_2")
    def ac_monthly_3 = column[Float]("ac_monthly_3")
    def ac_monthly_4 = column[Float]("ac_monthly_4")
    def ac_monthly_5 = column[Float]("ac_monthly_5")
    def ac_monthly_6 = column[Float]("ac_monthly_6")
    def ac_monthly_7 = column[Float]("ac_monthly_7")
    def ac_monthly_8 = column[Float]("ac_monthly_8")
    def ac_monthly_9 = column[Float]("ac_monthly_9")
    def ac_monthly_10 = column[Float]("ac_monthly_10")
    def ac_monthly_11 = column[Float]("ac_monthly_11")
    def ac_monthly_12 = column[Float]("ac_monthly_12")
    def ac_annual = column[Float]("ac_annual")
    def solrad_annual = column[Float]("solrad_annual")
    def capacity_factor = column[Float]("capacity_factor")

    private type SolarYieldTupleType = (
      Int,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float,
      Float
      )
    //
    private val formShapedValue = (zip_code, ac_monthly_1, ac_monthly_2, ac_monthly_3, ac_monthly_4, ac_monthly_5,
      ac_monthly_6, ac_monthly_7 ,ac_monthly_8, ac_monthly_9, ac_monthly_10, ac_monthly_11, ac_monthly_12,
      ac_annual, solrad_annual, capacity_factor).shaped[SolarYieldTupleType]
    //
    private val toModel: SolarYieldTupleType => SolarYield = { solarYieldTuple =>
      SolarYield(
        zip_code = solarYieldTuple._1,
        ac_monthly_1 = solarYieldTuple._2,
        ac_monthly_2 = solarYieldTuple._3,
        ac_monthly_3 = solarYieldTuple._4,
        ac_monthly_4 = solarYieldTuple._5,
        ac_monthly_5 = solarYieldTuple._6,
        ac_monthly_6 = solarYieldTuple._7,
        ac_monthly_7 = solarYieldTuple._8,
        ac_monthly_8 = solarYieldTuple._9,
        ac_monthly_9 = solarYieldTuple._10,
        ac_monthly_10 = solarYieldTuple._11,
        ac_monthly_11 = solarYieldTuple._12,
        ac_monthly_12 = solarYieldTuple._13,
        ac_annual = solarYieldTuple._14,
        solrad_annual = solarYieldTuple._15,
        capacity_factor = solarYieldTuple._16
      )
    }
    private val toTuple: SolarYield => Option[SolarYieldTupleType] = { solarYield =>
      Some {
        (
          solarYield.zip_code,
          solarYield.ac_monthly_1,
          solarYield.ac_monthly_2,
          solarYield.ac_monthly_3,
          solarYield.ac_monthly_4,
          solarYield.ac_monthly_5,
          solarYield.ac_monthly_6,
          solarYield.ac_monthly_7,
          solarYield.ac_monthly_8,
          solarYield.ac_monthly_9,
          solarYield.ac_monthly_10,
          solarYield.ac_monthly_11,
          solarYield.ac_monthly_12,
          solarYield.ac_annual,
          solarYield.solrad_annual,
          solarYield.capacity_factor
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}