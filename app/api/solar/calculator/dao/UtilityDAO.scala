package api.solar.calculator.dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import api.solar.calculator.models.Utility
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class UtilityDAO @Inject()(@NamedDatabase("solarcalculator") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Utilities = TableQuery[UtilityTable]

  def all(): Future[Seq[Utility]] = db.run(Utilities.result)

  def insert(utility: Utility): Future[Utility] = db.run(Utilities returning Utilities.map(_.id) into ((e, id) => e.copy(id = id)) += utility)

  def batchInsert(utilities: Seq[Utility]): Future[Unit] = db.run(Utilities ++= utilities).map { _ => () }

  def update(utility: Utility): Future[Unit] = {
    val formToUpdate = utility.copy(utility.id)
    db.run(Utilities.filter(_.id === utility.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Utilities.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Utility]] = {
    db.run(Utilities.filter(_.id === id).result.headOption)
  }

  def findByStateAndUtilityCompany(state: String, utility_company: String): Future[Option[Utility]] = {
    db.run(Utilities.filter(x => (x.state === state) && (x.name === utility_company)).result.headOption)
  }

  private class UtilityTable(tag: Tag) extends Table[Utility](tag, "utilities") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def state = column[String]("state")
    def sum_of_customers = column[Long]("sum_of_customers")
    def sum_of_sales = column[Long]("sum_of_sales")
    def sum_of_revenues = column[Long]("sum_of_revenues")
    def average_kwh_per_user = column[BigDecimal]("average_kwh_per_user")
    def cost = column[Float]("cost")
    
    private type UtilityTupleType = (
      Long,
      String,
      String,
      Long,
      Long,
      Long,
      BigDecimal,
      Float
    )
    //
    private val formShapedValue = (id, name, state, sum_of_customers, sum_of_sales, sum_of_revenues, average_kwh_per_user, cost).shaped[UtilityTupleType]
    //
    private val toModel: UtilityTupleType => Utility = { utilityTuple =>
      Utility(
        id = utilityTuple._1,
        name = utilityTuple._2,
        state = utilityTuple._3,
        sum_of_customers = utilityTuple._4,
        sum_of_sales = utilityTuple._5,
        sum_of_revenues = utilityTuple._6,
        average_kwh_per_user = utilityTuple._7,
        cost = utilityTuple._8
      )
    }
    private val toTuple: Utility => Option[UtilityTupleType] = { utility =>
      Some {
        (
          utility.id,
          utility.name,
          utility.state,
          utility.sum_of_customers,
          utility.sum_of_sales,
          utility.sum_of_revenues,
          utility.average_kwh_per_user,
          utility.cost
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}