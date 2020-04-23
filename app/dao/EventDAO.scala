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

class EventDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Events = TableQuery[EventTable]

  def all(): Future[Seq[Event]] = db.run(Events.result)

  def insert(event: Event): Future[Event] = db.run(Events returning Events.map(_.id) into ((e, id) => e.copy(id = id)) += event)

  def batchInsert(events: Seq[Event]): Future[Unit] = db.run(Events ++= events).map { _ => () }

  def update(event: Event): Future[Unit] = {
    val formToUpdate = event.copy(event.id)
    db.run(Events.filter(_.id === event.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Events.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Event]] = {
    db.run(Events.filter(_.id === id).result.headOption)
  }

  def findByEventId(event_id: Long): Future[Option[Event]] = {
    db.run(Events.filter(_.id === event_id).result.headOption)
  }

  private class EventTable(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def event_type_id = column[Option[Long]]("event_type_id")
    def session_id = column[Option[Long]]("session_id")
    def parent_event_id = column[Option[Long]]("parent_event_id")
    def url_id = column[Option[Long]]("url_id")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type EventTupleType = (
      Option[Long],
      Option[Long],
      Option[Long],
      Option[Long],
      Option[Long],
      Option[DateTime],
      Option[DateTime]
    )
    //
    private val formShapedValue = (id, event_type_id, session_id, parent_event_id, url_id, created_at, updated_at).shaped[EventTupleType]
    //
    private val toModel: EventTupleType => Event = { eventTuple =>
      Event(
        id = eventTuple._1,
        event_type_id = eventTuple._2,
        session_id = eventTuple._3,
        parent_event_id = eventTuple._4,
        url_id = eventTuple._5,
        created_at = eventTuple._6,
        updated_at = eventTuple._7
      )
    }
    private val toTuple: Event => Option[EventTupleType] = { event =>
      Some {
        (
          event.id,
          event.event_type_id,
          event.session_id,
          event.parent_event_id,
          event.url_id,
          event.created_at,
          event.updated_at
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}