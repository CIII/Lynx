package dao.abtest

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models.abtest.ABTestMember
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ABTestMemberDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val ABTestMembers = TableQuery[ABTestMemberTable]

  def all(): Future[Seq[ABTestMember]] = db.run(ABTestMembers.result)

  def insert(abTestMember: ABTestMember): Future[ABTestMember] = db.run(ABTestMembers returning ABTestMembers.map(_.id) into ((et, id) => et.copy(id = id)) += abTestMember)

  def batchInsert(abTestMembers: Seq[ABTestMember]): Future[Unit] = db.run(ABTestMembers ++= abTestMembers).map { _ => () }

  def update(abTestMember: ABTestMember): Future[Unit] = {
    val formToUpdate = abTestMember.copy(abTestMember.id)
    db.run(ABTestMembers.filter(_.id === abTestMember.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(ABTestMembers.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[ABTestMember]] = {
    db.run(ABTestMembers.filter(_.id === id).result.headOption)
  }

  def findByEventAndTestId(event_id: Long, ab_test_id: Long): Future[Option[ABTestMember]] = {
    db.run(ABTestMembers.filter(x => (x.event_id === event_id && x.ab_test_id === ab_test_id)).result.headOption)
  }

  private class ABTestMemberTable(tag: Tag) extends Table[ABTestMember](tag, "ab_test_members") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def event_id = column[Long]("event_id")
    def session_id = column[Option[Long]]("session_id")
    def ab_test_id = column[Long]("ab_test_id")
    def value = column[Option[String]]("value")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type ABTestMemberTupleType = (
      Long,
      Long,
      Option[Long],
      Long,
      Option[DateTime],
      Option[DateTime]
      )
    //
    private val formShapedValue = (id, event_id, session_id, ab_test_id, created_at, updated_at).shaped[ABTestMemberTupleType]
    //
    private val toModel: ABTestMemberTupleType => ABTestMember = { abTestMemberTuple =>
      ABTestMember(
        id = abTestMemberTuple._1,
        event_id = abTestMemberTuple._2,
        session_id = abTestMemberTuple._3,
        ab_test_id = abTestMemberTuple._4,
        created_at = abTestMemberTuple._5,
        updated_at = abTestMemberTuple._6
      )
    }
    private val toTuple: ABTestMember => Option[ABTestMemberTupleType] = { abTestMember =>
      Some {
        (
          abTestMember.id,
          abTestMember.event_id,
          abTestMember.session_id,
          abTestMember.ab_test_id,
          abTestMember.created_at,
          abTestMember.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}