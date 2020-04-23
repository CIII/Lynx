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

class EventTypeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val EventTypes = TableQuery[EventTypeTable]

  def all(): Future[Seq[EventType]] = db.run(EventTypes.result)

  //def insert(eventType: EventType): Future[Unit] = db.run(EventTypes += eventType).map { _ => () }

  def insert(eventType: models.EventType): Future[models.EventType] = db.run(EventTypes returning EventTypes.map(_.id) into ((et, id) => et.copy(id = id)) += eventType)

  def batchInsert(eventTypes: Seq[EventType]): Future[Unit] = db.run(EventTypes ++= eventTypes).map { _ => () }

  def update(eventType: EventType): Future[Unit] = {
    val formToUpdate = eventType.copy(eventType.id)
    db.run(EventTypes.filter(_.id === eventType.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(EventTypes.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[EventType]] = {
    db.run(EventTypes.filter(_.id.get === id).result.headOption)
  }

  def findByEventName(name: String): Future[Option[EventType]] = {
    db.run(EventTypes.filter(_.name === name).result.headOption)
  }

  def findByEventTypeId(id: Long): Future[Option[EventType]] = {
    db.run(EventTypes.filter(_.id.get === id).result.headOption)
  }

  private class EventTypeTable(tag: Tag) extends Table[EventType](tag, "event_types") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def event_category = column[String]("event_category")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type EventTypeTupleType = (
      Option[Long],
      String,
      String,
      Option[DateTime],
      Option[DateTime]
      )
    //
    private val formShapedValue = (id, name, event_category, created_at, updated_at).shaped[EventTypeTupleType]
    //
    private val toModel: EventTypeTupleType => EventType = { eventTypeTuple =>
      EventType(
        id = eventTypeTuple._1,
        name = eventTypeTuple._2,
        event_category = eventTypeTuple._3,
        created_at = eventTypeTuple._4,
        updated_at = eventTypeTuple._5
      )
    }
    private val toTuple: EventType => Option[EventTypeTupleType] = { eventType =>
      Some {
        (
          eventType.id,
          eventType.name,
          eventType.event_category,
          eventType.created_at,
          eventType.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}