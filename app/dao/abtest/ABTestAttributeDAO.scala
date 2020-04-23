package dao.abtest

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import models.abtest._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ABTestAttributeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val ABTestAttributes = TableQuery[ABTestAttributeTable]

  def all(): Future[Seq[ABTestAttribute]] = db.run(ABTestAttributes.result)
  
  def insert(attribute: ABTestAttribute): Future[ABTestAttribute] = db.run(ABTestAttributes returning ABTestAttributes.map(_.id) into ((a, id) => a.copy(id = id)) += attribute)

  def batchInsert(attributes: Seq[ABTestAttribute]): Future[Unit] = db.run(ABTestAttributes ++= attributes).map { _ => () }

  def update(name: ABTestAttribute): Future[Unit] = {
    val formToUpdate = name.copy(name.id)
    db.run(ABTestAttributes.filter(_.id === name.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(ABTestAttributes.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[ABTestAttribute]] = {
    db.run(ABTestAttributes.filter(_.id === id).result.headOption)
  }

  def findByName(name: String): Future[Option[ABTestAttribute]] = {
    db.run(ABTestAttributes.filter(_.name === name).result.headOption)
  }

  def findByABTestAttributeId(id: Long): Future[Option[ABTestAttribute]] = {
    db.run(ABTestAttributes.filter(_.id === id).result.headOption)
  }

  private class ABTestAttributeTable(tag: Tag) extends Table[ABTestAttribute](tag, "ab_test_attributes") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type ABTestAttributeTupleType = (
        Long,
        String,
        Option[DateTime],
        Option[DateTime]
      )
    //
    private val formShapedValue = (id, name, created_at, updated_at).shaped[ABTestAttributeTupleType]
    //
    private val toModel: ABTestAttributeTupleType => ABTestAttribute = { attributeTuple =>
      ABTestAttribute(
        id = attributeTuple._1,
        name = attributeTuple._2,
        created_at = attributeTuple._3,
        updated_at = attributeTuple._4
      )
    }
    private val toTuple: ABTestAttribute => Option[ABTestAttributeTupleType] = { attribute =>
      Some {
        (
          attribute.id,
          attribute.name,
          attribute.created_at,
          attribute.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}