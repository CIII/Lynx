package dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class DomainDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Domains = TableQuery[DomainTable]

  def all(): Future[Seq[Domain]] = db.run(Domains.result)

  def insert(domain: models.Domain): Future[models.Domain] = db.run(Domains returning Domains.map(_.id) into ((b, id) => b.copy(id = id)) += domain)

  def batchInsert(domains: Seq[Domain]): Future[Unit] = db.run(Domains ++= domains).map { _ => () }

  def delete(id: Long): Future[Unit] = db.run(Domains.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[Domain]] = {
    db.run(Domains.filter(_.id.get === id).result.headOption)
  }

  def findByDomainId(domain: String): Future[Option[Domain]] = {
    db.run(Domains.filter(_.domain === domain).result.headOption)
  }

  private class DomainTable(tag: Tag) extends Table[Domain](tag, "domains") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def domain = column[String]("domain")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type DomainTupleType = (
      Option[Long],
      String,
      Option[DateTime],
      Option[DateTime]
    )
    //
    private val formShapedValue = (id, domain, created_at, updated_at).shaped[DomainTupleType]
    //
    private val toModel: DomainTupleType => Domain = { domainTuple =>
      Domain(
        id = domainTuple._1,
        domain = domainTuple._2,
        created_at = domainTuple._3,
        updated_at = domainTuple._4
      )
    }
    private val toTuple: Domain => Option[DomainTupleType] = { domain =>
      Some {
        (
          domain.id,
          domain.domain,
          domain.created_at,
          domain.updated_at
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}