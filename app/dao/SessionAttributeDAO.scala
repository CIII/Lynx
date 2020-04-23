package dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class SessionAttributeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val SessionAttributes = TableQuery[SessionAttributeTable]

  def all(): Future[Seq[SessionAttribute]] = db.run(SessionAttributes.result)

  def insert(sessionAttribute: models.SessionAttribute): Future[models.SessionAttribute] = db.run(SessionAttributes returning SessionAttributes.map(_.id) into ((et, id) => et.copy(id = id)) += sessionAttribute)

  def batchInsert(sessionAttributes: Seq[SessionAttribute]): Future[Unit] = db.run(SessionAttributes ++= sessionAttributes).map { _ => () }

  def update(sessionAttribute: SessionAttribute): Future[Unit] = {
    val formToUpdate = sessionAttribute.copy(sessionAttribute.id)
    db.run(SessionAttributes.filter(_.id === sessionAttribute.id).update(formToUpdate)).map(_ => ())
  }

  def updateValueIfExists(session_id: Long, attribute_id: Long, value: String): Future[Unit] ={
    findBySessionIdAndAttribute(session_id, attribute_id).map {
      // Found time to update
      case Some(sessionAttribute) =>
        sessionAttribute.value = Some(value)
        update(sessionAttribute)
      // Not found, time to add
      case None =>
        val current_time = Some(new DateTime(DateTimeZone.UTC))
        insert(SessionAttribute(
          id = Some(0L),
          session_id = Some(session_id),
          attribute_id = Some(attribute_id),
          value = Some(value),
          created_at = current_time,
          updated_at =  current_time
        ))
    }
  }

  def delete(id: Long): Future[Unit] = db.run(SessionAttributes.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[SessionAttribute]] = {
    db.run(SessionAttributes.filter(_.id === id).result.headOption)
  }

  def findBySessionIdAndAttribute(session_id: Long, attribute_id: Long): Future[Option[SessionAttribute]] = {
    db.run(SessionAttributes.filter(x => (x.session_id === session_id && x.attribute_id === attribute_id)).result.headOption)
  }
  
  def findBySessionIdAndAttributes(session_id: Long, attribute_ids: List[Long]): Future[Seq[SessionAttribute]] = {
    val q = for {
      sa <- SessionAttributes
      if sa.session_id === session_id
      if sa.attribute_id inSetBind attribute_ids
    } yield (sa)
    val dbFuture = db.run(q.result)
    dbFuture.recover({ case e: SlickException => Seq.empty[SessionAttribute] })
  }

  private class SessionAttributeTable(tag: Tag) extends Table[SessionAttribute](tag, "session_attributes") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def session_id = column[Option[Long]]("session_id")
    def attribute_id = column[Option[Long]]("attribute_id")
    def value = column[Option[String]]("value")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type SessionAttributeTupleType = (
      Option[Long],
      Option[Long],
      Option[Long],
      Option[String],
      Option[DateTime],
      Option[DateTime]
      )
    //
    private val formShapedValue = (id, session_id, attribute_id, value, created_at, updated_at).shaped[SessionAttributeTupleType]
    //
    private val toModel: SessionAttributeTupleType => SessionAttribute = { sessionAttributeTuple =>
      SessionAttribute(
        id = sessionAttributeTuple._1,
        session_id = sessionAttributeTuple._2,
        attribute_id = sessionAttributeTuple._3,
        value = sessionAttributeTuple._4,
        created_at = sessionAttributeTuple._5,
        updated_at = sessionAttributeTuple._6
      )
    }
    private val toTuple: SessionAttribute => Option[SessionAttributeTupleType] = { sessionAttribute =>
      Some {
        (
          sessionAttribute.id,
          sessionAttribute.session_id,
          sessionAttribute.attribute_id,
          sessionAttribute.value,
          sessionAttribute.created_at,
          sessionAttribute.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}