package dao.abtest

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models.abtest._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ABTestDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val ABTests = TableQuery[ABTestTable]

  def all(): Future[Seq[ABTest]] = db.run(ABTests.result)

  def insert(abTest: ABTest): Future[ABTest] = db.run(ABTests returning ABTests.map(_.id) into ((u, id) => u.copy(id = id)) += abTest)

  def batchInsert(abTests: Seq[ABTest]): Future[Unit] = db.run(ABTests ++= abTests).map { _ => () }

  def update(abTest: ABTest): Future[Unit] = {
    val formToUpdate = abTest.copy(abTest.id)
    db.run(ABTests.filter(_.id === abTest.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(ABTests.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[ABTest]] = {
    db.run(ABTests.filter(_.id === id).result.headOption)
  }

  def findByName(name: String): Future[Option[ABTest]] = {
    db.run(ABTests.filter(_.name === name).result.headOption)
  }

  private class ABTestTable(tag: Tag) extends Table[ABTest](tag, "ab_tests") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type ABTestTupleType = (
      Long,
      String,
      Option[String],
      Option[DateTime],
      Option[DateTime]
    )
    //
    private val formShapedValue = (id, name, description, created_at, updated_at).shaped[ABTestTupleType]
    //
    private val toModel: ABTestTupleType => ABTest = { abTestTuple =>
      ABTest(
        id = abTestTuple._1,
        name = abTestTuple._2,
        description = abTestTuple._3,
        created_at = abTestTuple._4,
        updated_at = abTestTuple._5
      )
    }
    private val toTuple: ABTest => Option[ABTestTupleType] = { abTest =>
      Some {
        (
          abTest.id,
          abTest.name,
          abTest.description,
          abTest.created_at,
          abTest.updated_at
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}