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

class BrowserDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Browsers = TableQuery[BrowserTable]

  def all(): Future[Seq[Browser]] = db.run(Browsers.result)

  def insert(browser: models.Browser): Future[models.Browser] = db.run(Browsers returning Browsers.map(_.id) into ((b, id) => b.copy(id = id)) += browser)

  def batchInsert(browsers: Seq[Browser]): Future[Unit] = db.run(Browsers ++= browsers).map { _ => () }

  def update(browser: Browser): Future[Unit] = {
    val formToUpdate = browser.copy(browser.id)
    db.run(Browsers.filter(_.id === browser.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Browsers.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Browser]] = {
    db.run(Browsers.filter(_.id.get === id).result.headOption)
  }

  def findByBrowserId(browser_id: String): Future[Option[Browser]] = {
    db.run(Browsers.filter(_.browser_id === browser_id).result.headOption)
  }

  private class BrowserTable(tag: Tag) extends Table[Browser](tag, "browsers") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def browser_id = column[Option[String]]("browser_id")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type BrowserTupleType = (
      Option[Long],
      Option[String],
      Option[DateTime],
      Option[DateTime]
    )
    //
    private val formShapedValue = (id, browser_id, created_at, updated_at).shaped[BrowserTupleType]
    //
    private val toModel: BrowserTupleType => Browser = { browserTuple =>
      Browser(
        id = browserTuple._1,
        browser_id = browserTuple._2,
        created_at = browserTuple._3,
        updated_at = browserTuple._4
      )
    }
    private val toTuple: Browser => Option[BrowserTupleType] = { browser =>
      Some {
        (
          browser.id,
          browser.browser_id,
          browser.created_at,
          browser.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}