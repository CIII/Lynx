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

class ApiTokenDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val ApiTokens = TableQuery[ApiTokenTable]

  def all(): Future[Seq[ApiToken]] = db.run(ApiTokens.result)

  def insert(apiToken: models.ApiToken): Future[models.ApiToken] = db.run(ApiTokens returning ApiTokens.map(_.id) into ((u, id) => u.copy(id = id)) += apiToken)

  def batchInsert(apiTokens: Seq[ApiToken]): Future[Unit] = db.run(ApiTokens ++= apiTokens).map { _ => () }

  def update(apiToken: ApiToken): Future[Unit] = {
    val formToUpdate = apiToken.copy(apiToken.id)
    db.run(ApiTokens.filter(_.id === apiToken.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(ApiTokens.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[ApiToken]] = {
    db.run(ApiTokens.filter(_.id.get === id).result.headOption)
  }

  def findByToken(token: String): Future[Option[ApiToken]] = {
    db.run(ApiTokens.filter(_.token === token).result.headOption)
  }

  private class ApiTokenTable(tag: Tag) extends Table[ApiToken](tag, "api_tokens") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def token = column[String]("token")
    def active = column[Boolean]("active")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type ApiTokenTupleType = (
      Option[Long],
        String,
        Boolean,
        Option[DateTime],
        Option[DateTime]
      )
    //
    private val formShapedValue = (id, token, active, created_at, updated_at).shaped[ApiTokenTupleType]
    //
    private val toModel: ApiTokenTupleType => ApiToken = { apiTokenTuple =>
      ApiToken(
        id = apiTokenTuple._1,
        token = apiTokenTuple._2,
        active = apiTokenTuple._3,
        created_at = apiTokenTuple._4,
        updated_at = apiTokenTuple._5
      )
    }
    private val toTuple: ApiToken => Option[ApiTokenTupleType] = { apiToken =>
      Some {
        (
          apiToken.id,
          apiToken.token,
          apiToken.active,
          apiToken.created_at,
          apiToken.updated_at
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}