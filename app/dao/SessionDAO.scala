package dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class SessionDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Sessions = TableQuery[SessionTable]

  def all(): Future[Seq[models.Session]] = db.run(Sessions.result)

  //def insert(session: Session): Future[Unit] = db.run(Sessions += session).map { _ => () }

  def insert(session: models.Session): Future[models.Session] = db.run(Sessions returning Sessions.map(_.id) into ((s, id) => s.copy(id = id)) += session)

  def batchInsert(sessions: Seq[models.Session]): Future[Unit] = db.run(Sessions ++= sessions).map { _ => () }

  def update(session: models.Session): Future[Unit] = {
    val formToUpdate = session.copy(session.id)
    db.run(Sessions.filter(_.id === session.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Sessions.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[models.Session]] = {
    db.run(Sessions.filter(_.id === id).result.headOption)
  }

  def findBySessionId(id: Long): Future[Option[models.Session]] = {
    db.run(Sessions.filter(_.id === id).result.headOption)
  }

  def touch(id: Long): Unit = {
    db.run(Sessions.filter(_.id === id).map(s => s.last_activity).update(Some(new DateTime(DateTimeZone.UTC))))
  }

  private class SessionTable(tag: Tag) extends Table[models.Session](tag, "sessions") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def browser_id =  column[Option[Long]]("browser_id")
    def domain_id =  column[Option[Long]]("domain_id")
    def ip =  column[Option[String]]("ip")
    def user_agent =  column[Option[String]]("user_agent")
    def traffic_source_id =  column[Option[Long]]("traffic_source_id")
    def is_robot = column[Boolean]("is_robot")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")
    def last_activity = column[Option[DateTime]]("last_activity")

    private type SessionTupleType = (
      Option[Long],
      Option[Long],
      Option[Long],
      Option[String],
      Option[String],
      Option[Long],
      Boolean,
      Option[DateTime],
      Option[DateTime],
      Option[DateTime]
      )
    //
    private val formShapedValue = (id, browser_id, domain_id, ip, user_agent, traffic_source_id, is_robot, created_at,
      updated_at, last_activity).shaped[SessionTupleType]
    //
    private val toModel: SessionTupleType => models.Session = { sessionTuple =>
      Session(
        id = sessionTuple._1,
        browser_id = sessionTuple._2,
        domain_id = sessionTuple._3,
        ip = sessionTuple._4,
        user_agent = sessionTuple._5,
        traffic_source_id = sessionTuple._6,
        is_robot = sessionTuple._7,
        created_at = sessionTuple._8,
        updated_at = sessionTuple._9,
        last_activity = sessionTuple._10
      )
    }
    private val toTuple: models.Session => Option[SessionTupleType] = { session =>
      Some {
        (
          session.id,
          session.browser_id,
          session.domain_id,
          session.ip,
          session.user_agent,
          session.traffic_source_id,
          session.is_robot,
          session.created_at,
          session.updated_at,
          session.last_activity
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}