package api.solar.calculator.dao

import javax.inject.Inject

import api.solar.calculator.models.CostByState
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class CostByStateDAO @Inject()(@NamedDatabase("solarcalculator") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val CostByStates = TableQuery[CostByStateTable]

  def all(): Future[Seq[CostByState]] = db.run(CostByStates.result)

  def insert(costByState: CostByState): Future[CostByState] = db.run(CostByStates returning CostByStates.map(_.state) into ((e, state) => e.copy(state = state)) += costByState)

  def batchInsert(costByStates: Seq[CostByState]): Future[Unit] = db.run(CostByStates ++= costByStates).map { _ => () }

  def update(costByState: CostByState): Future[Unit] = {
    val formToUpdate = costByState.copy(costByState.state)
    db.run(CostByStates.filter(_.state === costByState.state).update(formToUpdate)).map(_ => ())
  }

  def delete(state: String): Future[Unit] = db.run(CostByStates.filter(_.state === state).delete).map(_ => ())

  def find(state: String): Future[Option[CostByState]] = {
    db.run(CostByStates.filter(_.state === state).result.headOption)
  }

  private class CostByStateTable(tag: Tag) extends Table[CostByState](tag, "cost_by_states") {
    def state = column[String]("state", O.PrimaryKey, O.AutoInc)
    def sum_of_customers = column[Long]("sum_of_customers")
    def sum_of_sales = column[Long]("sum_of_sales")
    def sum_of_revenues = column[Long]("sum_of_revenues")
    def average_kwh_per_user = column[BigDecimal]("average_kwh_per_user")
    def cost = column[Float]("cost")
    
    private type CostByStateTupleType = (
      String,
      Long,
      Long,
      Long,
      BigDecimal,
      Float
    )
    //
    private val formShapedValue = (state, sum_of_customers, sum_of_sales, sum_of_revenues, average_kwh_per_user, cost).shaped[CostByStateTupleType]
    //
    private val toModel: CostByStateTupleType => CostByState = { costByStateTuple =>
      CostByState(
        state = costByStateTuple._1,
        sum_of_customers = costByStateTuple._2,
        sum_of_sales = costByStateTuple._3,
        sum_of_revenues = costByStateTuple._4,
        average_kwh_per_user = costByStateTuple._5,
        cost = costByStateTuple._6
      )
    }
    private val toTuple: CostByState => Option[CostByStateTupleType] = { costByState =>
      Some {
        (
          costByState.state,
          costByState.sum_of_customers,
          costByState.sum_of_sales,
          costByState.sum_of_revenues,
          costByState.average_kwh_per_user,
          costByState.cost
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}