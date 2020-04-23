package dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class RevenueDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Revenues = TableQuery[RevenueTable]

  def all(): Future[Seq[Revenue]] = db.run(Revenues.result)

  def insert(revenue: Revenue): Future[Revenue] = db.run(Revenues returning Revenues.map(_.id) into ((e, id) => e.copy(id = id)) += revenue)

  def batchInsert(revenues: Seq[Revenue]): Future[Unit] = db.run(Revenues ++= revenues).map { _ => () }

  def update(revenue: Revenue): Future[Unit] = {
    val formToUpdate = revenue.copy(revenue.id)
    db.run(Revenues.filter(_.id === revenue.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Revenues.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Revenue]] = {
    db.run(Revenues.filter(_.id === id).result.headOption)
  }

  def findByRevenueId(revenue_id: Long): Future[Option[Revenue]] = {
    db.run(Revenues.filter(_.id === revenue_id).result.headOption)
  }
  
  def findBySessionId(session_id: Long): Future[Option[Revenue]] = {
    db.run(Revenues.filter(_.session_id === session_id).result.headOption)
  }

  private class RevenueTable(tag: Tag) extends Table[Revenue](tag, "revenues") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def session_id = column[Long]("session_id")
    def total_revenue = column[BigDecimal]("total_revenue")
    def con_f = column[Long]("con_f")
    def created_at = column[DateTime]("created_at")
    def updated_at = column[DateTime]("updated_at")

    private type RevenueTupleType = (
      Long,
      Long,
      BigDecimal,
      Long,
      DateTime,
      DateTime
    )
    //
    private val formShapedValue = (id, session_id, total_revenue, con_f, created_at, updated_at).shaped[RevenueTupleType]
    //
    private val toModel: RevenueTupleType => Revenue = { revenueTuple =>
      Revenue(
        id = revenueTuple._1,
        session_id = revenueTuple._2,
        total_revenue = revenueTuple._3,
        con_f = revenueTuple._4,
        created_at = revenueTuple._5,
        updated_at = revenueTuple._6
      )
    }
    private val toTuple: Revenue => Option[RevenueTupleType] = { revenue =>
      Some {
        (
          revenue.id,
          revenue.session_id,
          revenue.total_revenue,
          revenue.con_f,
          revenue.created_at,
          revenue.updated_at
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}