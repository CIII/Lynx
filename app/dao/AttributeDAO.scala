package dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class AttributeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Attributes = TableQuery[AttributeTable]

  def all(): Future[Seq[Attribute]] = db.run(Attributes.result)
  
  def insert(attribute: models.Attribute): Future[models.Attribute] = db.run(Attributes returning Attributes.map(_.id) into ((a, id) => a.copy(id = id)) += attribute)

  def batchInsert(attributes: Seq[Attribute]): Future[Unit] = db.run(Attributes ++= attributes).map { _ => () }

  def update(name: Attribute): Future[Unit] = {
    val formToUpdate = name.copy(name.id)
    db.run(Attributes.filter(_.id === name.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Attributes.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Attribute]] = {
    db.run(Attributes.filter(_.id.get === id).result.headOption)
  }

  def findByAttribute(name: String): Future[Option[Attribute]] = {
    db.run(Attributes.filter(_.name === name).result.headOption)
  }

  def findByAttributeId(id: Long): Future[Option[Attribute]] = {
    db.run(Attributes.filter(_.id.get === id).result.headOption)
  }

  private class AttributeTable(tag: Tag) extends Table[Attribute](tag, "attributes") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type AttributeTupleType = (
        Option[Long],
        String,
        Option[DateTime],
        Option[DateTime]
      )
    //
    private val formShapedValue = (id, name, created_at, updated_at).shaped[AttributeTupleType]
    //
    private val toModel: AttributeTupleType => Attribute = { nameTuple =>
      Attribute(
        id = nameTuple._1,
        name = nameTuple._2,
        created_at = nameTuple._3,
        updated_at = nameTuple._4
      )
    }
    private val toTuple: Attribute => Option[AttributeTupleType] = { name =>
      Some {
        (
          name.id,
          name.name,
          name.created_at,
          name.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}