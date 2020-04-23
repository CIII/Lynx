package dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class EventAttributeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val EventAttributes = TableQuery[EventAttributeTable]

  def all(): Future[Seq[EventAttribute]] = db.run(EventAttributes.result)

  //def insert(eventAttribute: EventAttribute): Future[Unit] = db.run(EventAttributes += eventAttribute).map { _ => () }

  def insert(eventAttribute: models.EventAttribute): Future[models.EventAttribute] = db.run(EventAttributes returning EventAttributes.map(_.id) into ((et, id) => et.copy(id = id)) += eventAttribute)

  def batchInsert(eventAttributes: Seq[EventAttribute]): Future[Unit] = db.run(EventAttributes ++= eventAttributes).map { _ => () }

  def update(eventAttribute: EventAttribute): Future[Unit] = {
    val formToUpdate = eventAttribute.copy(eventAttribute.id)
    db.run(EventAttributes.filter(_.id === eventAttribute.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(EventAttributes.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[EventAttribute]] = {
    db.run(EventAttributes.filter(_.id.get === id).result.headOption)
  }

  def findByEventAttributeId(id: Long): Future[Option[EventAttribute]] = {
    db.run(EventAttributes.filter(_.id.get === id).result.headOption)
  }
  
  def findByEventIdAndAttribute(event_id: Long, attribute_id: Long): Future[Option[EventAttribute]] = {
    db.run(EventAttributes.filter(x => (x.event_id === event_id && x.attribute_id === attribute_id)).result.headOption)
  }
  
  private class EventAttributeTable(tag: Tag) extends Table[EventAttribute](tag, "event_attributes") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def event_id = column[Option[Long]]("event_id")
    def attribute_id = column[Option[Long]]("attribute_id")
    def value = column[Option[String]]("value")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type EventAttributeTupleType = (
      Option[Long],
      Option[Long],
      Option[Long],
      Option[String],
      Option[DateTime],
      Option[DateTime]
      )
    //
    private val formShapedValue = (id, event_id, attribute_id, value, created_at, updated_at).shaped[EventAttributeTupleType]
    //
    private val toModel: EventAttributeTupleType => EventAttribute = { eventAttributeTuple =>
      EventAttribute(
        id = eventAttributeTuple._1,
        event_id = eventAttributeTuple._2,
        attribute_id = eventAttributeTuple._3,
        value = eventAttributeTuple._4,
        created_at = eventAttributeTuple._5,
        updated_at = eventAttributeTuple._6
      )
    }
    private val toTuple: EventAttribute => Option[EventAttributeTupleType] = { eventAttribute =>
      Some {
        (
          eventAttribute.id,
          eventAttribute.event_id,
          eventAttribute.attribute_id,
          eventAttribute.value,
          eventAttribute.created_at,
          eventAttribute.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}