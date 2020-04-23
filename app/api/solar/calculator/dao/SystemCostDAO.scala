package api.solar.calculator.dao

import javax.inject.Inject

import api.solar.calculator.models.SystemCost
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class SystemCostDAO @Inject()(@NamedDatabase("solarcalculator") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val SystemCosts = TableQuery[SystemCostTable]

  def all(): Future[Seq[SystemCost]] = db.run(SystemCosts.result)

  def insert(srec: SystemCost): Future[SystemCost] = db.run(SystemCosts returning SystemCosts.map(_.state) into ((e, state) => e.copy(state = state)) += srec)

  def batchInsert(srecs: Seq[SystemCost]): Future[Unit] = db.run(SystemCosts ++= srecs).map { _ => () }

  def update(srec: SystemCost): Future[Unit] = {
    val formToUpdate = srec.copy(srec.state)
    db.run(SystemCosts.filter(_.state === srec.state).update(formToUpdate)).map(_ => ())
  }

  def find(state: String): Future[Option[SystemCost]] = {
    db.run(SystemCosts.filter(_.state === state).result.headOption)
  }

  def average_costs: Future[(Float, Float)] = {
    for {
      low <- db.run(SystemCosts.map(_.low).avg.result)
      high <- db.run(SystemCosts.map(_.high).avg.result)
    }yield(low.get, high.get)
  }

  private class SystemCostTable(tag: Tag) extends Table[SystemCost](tag, "system_costs") {
    def state = column[String]("state")
    def low = column[Float]("low")
    def high = column[Float]("high")

    private type SystemCostTupleType = (
      String,
      Float,
      Float
    )
    //
    private val formShapedValue = (state, low, high).shaped[SystemCostTupleType]
    //
    private val toModel: SystemCostTupleType => SystemCost = { srecTuple =>
      SystemCost(
        state = srecTuple._1,
        low = srecTuple._2,
        high = srecTuple._3
      )
    }
    private val toTuple: SystemCost => Option[SystemCostTupleType] = { srec =>
      Some {
        (
          srec.state,
          srec.low,
          srec.high
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}