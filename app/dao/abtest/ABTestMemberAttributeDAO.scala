package dao.abtest

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import models.abtest.ABTestMemberAttribute
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ABTestMemberAttributeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val ABTestMemberAttributes = TableQuery[ABTestMemberAttributeTable]

  def all(): Future[Seq[ABTestMemberAttribute]] = db.run(ABTestMemberAttributes.result)

  //def insert(abTestMemberAttribute: ABTestMemberAttribute): Future[Unit] = db.run(ABTestMemberAttributes += abTestMemberAttribute).map { _ => () }

  def insert(abTestMemberAttribute: ABTestMemberAttribute): Future[ABTestMemberAttribute] = db.run(ABTestMemberAttributes returning ABTestMemberAttributes.map(_.id) into ((et, id) => et.copy(id = id)) += abTestMemberAttribute)

  def batchInsert(abTestMemberAttributes: Seq[ABTestMemberAttribute]): Future[Unit] = db.run(ABTestMemberAttributes ++= abTestMemberAttributes).map { _ => () }

  def update(abTestMemberAttribute: ABTestMemberAttribute): Future[Unit] = {
    val formToUpdate = abTestMemberAttribute.copy(abTestMemberAttribute.id)
    db.run(ABTestMemberAttributes.filter(_.id === abTestMemberAttribute.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(ABTestMemberAttributes.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[ABTestMemberAttribute]] = {
    db.run(ABTestMemberAttributes.filter(_.id === id).result.headOption)
  }

  def findByABTestMemberAttributeId(id: Long): Future[Option[ABTestMemberAttribute]] = {
    db.run(ABTestMemberAttributes.filter(_.id === id).result.headOption)
  }
  
  def findByABTestMemberIdAndAttribute(ab_test_member_id: Long, ab_test_attribute_id: Long): Future[Option[ABTestMemberAttribute]] = {
    play.api.Logger("STEVE ITS LOOKING T_T")
    db.run(ABTestMemberAttributes.filter(x => (x.ab_test_member_id === ab_test_member_id && x.ab_test_attribute_id === ab_test_attribute_id)).result.headOption)
  }
  
  private class ABTestMemberAttributeTable(tag: Tag) extends Table[ABTestMemberAttribute](tag, "ab_test_member_attributes") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ab_test_member_id = column[Long]("ab_test_member_id")
    def ab_test_attribute_id = column[Long]("ab_test_attribute_id")
    def value = column[String]("value")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type ABTestMemberAttributeTupleType = (
      Long,
      Long,
      Long,
      String,
      Option[DateTime],
      Option[DateTime]
      )
    //
    private val formShapedValue = (id, ab_test_member_id, ab_test_attribute_id, value, created_at, updated_at).shaped[ABTestMemberAttributeTupleType]
    //
    private val toModel: ABTestMemberAttributeTupleType => ABTestMemberAttribute = { abTestMemberAttributeTuple =>
      ABTestMemberAttribute(
        id = abTestMemberAttributeTuple._1,
        ab_test_member_id = abTestMemberAttributeTuple._2,
        ab_test_attribute_id = abTestMemberAttributeTuple._3,
        value = abTestMemberAttributeTuple._4,
        created_at = abTestMemberAttributeTuple._5,
        updated_at = abTestMemberAttributeTuple._6
      )
    }
    private val toTuple: ABTestMemberAttribute => Option[ABTestMemberAttributeTupleType] = { abTestMemberAttribute =>
      Some {
        (
          abTestMemberAttribute.id,
          abTestMemberAttribute.ab_test_member_id,
          abTestMemberAttribute.ab_test_attribute_id,
          abTestMemberAttribute.value,
          abTestMemberAttribute.created_at,
          abTestMemberAttribute.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}