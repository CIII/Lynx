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

class UrlDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Urls = TableQuery[UrlTable]

  def all(): Future[Seq[Url]] = db.run(Urls.result)

  //def insert(url: Url): Future[Unit] = db.run(Urls += url).map { _ => () }

  def insert(url: models.Url): Future[models.Url] = db.run(Urls returning Urls.map(_.id) into ((u, id) => u.copy(id = id)) += url)

  def batchInsert(urls: Seq[Url]): Future[Unit] = db.run(Urls ++= urls).map { _ => () }

  def update(url: Url): Future[Unit] = {
    val formToUpdate = url.copy(url.id)
    db.run(Urls.filter(_.id === url.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Urls.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Url]] = {
    db.run(Urls.filter(_.id.get === id).result.headOption)
  }

  def findByUrl(url: String): Future[Option[Url]] = {
    db.run(Urls.filter(_.url === url).result.headOption)
  }

  def findByUrlId(id: Long): Future[Option[Url]] = {
    db.run(Urls.filter(_.id.get === id).result.headOption)
  }

  private class UrlTable(tag: Tag) extends Table[Url](tag, "urls") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def url = column[String]("url")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type UrlTupleType = (
        Option[Long],
        String,
        Option[DateTime],
        Option[DateTime]
      )
    //
    private val formShapedValue = (id, url, created_at, updated_at).shaped[UrlTupleType]
    //
    private val toModel: UrlTupleType => Url = { urlTuple =>
      Url(
        id = urlTuple._1,
        url = urlTuple._2,
        created_at = urlTuple._3,
        updated_at = urlTuple._4
      )
    }
    private val toTuple: Url => Option[UrlTupleType] = { url =>
      Some {
        (
          url.id,
          url.url,
          url.created_at,
          url.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}