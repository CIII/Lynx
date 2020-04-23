package dao

import java.util.UUID
import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import org.joda.time.{DateTime, DateTimeZone}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LeadDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Leads = TableQuery[LeadTable]

  def all(): Future[Seq[Lead]] = db.run(Leads.result)

  def insert(lead: Lead): Future[Lead] = {
    db.run(Leads += lead).map(res => lead)
    //db.run(Leads returning Leads.map(_.lead_id) into ((e, lead_id) => e.copy(lead_id = lead_id)) += lead)
  }

  def batchInsert(leads: Seq[Lead]): Future[Unit] = db.run(Leads ++= leads).map { _ => () }

  def delete(lead_id: Long): Future[Unit] = db.run(Leads.filter(_.lead_id === lead_id).delete).map(_ => ())

  def createIfNotExist(lead_id: Long, session_id: Long): Future[Lead] = {
    val current_time = new DateTime (DateTimeZone.UTC)
    findByLeadId(lead_id).map {
      case Some(lead) =>
        lead.updated_at = current_time;
        db.run(Leads.filter(_.lead_id === lead.lead_id).update(lead.copy(lead.lead_id))).map(_ => ())
        lead
      case None =>
        val lead = Lead(
          lead_id = lead_id,
          user_id = play.api.libs.Codecs.sha1(lead_id.toString).substring(0,8),
          session_id = session_id,
          created_at = current_time,
          updated_at = current_time
        )
        Await.result(insert(lead), Duration.Inf)
    }
  }

  def findByLeadId(lead_id: Long): Future[Option[Lead]] = {
    db.run(Leads.filter(_.lead_id === lead_id).result.headOption)
  }
  
  def findByUserId(user_id: String): Future[Option[Lead]] = {
    db.run(Leads.filter(_.user_id === user_id).result.headOption)
  }

  private class LeadTable(tag: Tag) extends Table[Lead](tag, "leads") {
    def lead_id = column[Long]("lead_id", O.PrimaryKey)
    def user_id = column[String]("user_id")
    def session_id = column[Long]("session_id")
    def created_at = column[DateTime]("created_at")
    def updated_at = column[DateTime]("updated_at")

    private type LeadTupleType = (
      Long,
      String,
      Long,
      DateTime,
      DateTime
    )
    //
    private val leadShapedValue = (lead_id, user_id, session_id, created_at, updated_at).shaped[LeadTupleType]
    //
    private val toModel: LeadTupleType => Lead = { leadTuple =>
      Lead(
        lead_id = leadTuple._1,
        user_id = leadTuple._2,
        session_id = leadTuple._3,
        created_at = leadTuple._4,
        updated_at = leadTuple._5
      )
    }
    private val toTuple: Lead => Option[LeadTupleType] = { lead =>
      Some {
        (
          lead.lead_id,
          lead.user_id,
          lead.session_id,
          lead.created_at,
          lead.updated_at
        )
      }
    }

    def * = leadShapedValue <> (toModel,toTuple)
  }
}