package security.dao

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import security.models.UserAccount
import slick.driver.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class UserAccountDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val UserAccounts = TableQuery[UserAccountTable]

  def delete(id: Long): Future[Unit] = db.run(UserAccounts.filter(_.id === id).delete).map(_ => ())

  def findByUserAccountId(id: Long): Future[Option[UserAccount]] = {
    db.run(UserAccounts.filter(_.id === id).result.headOption)
  }
  
  def findByUserName(user_name: String): Future[Option[UserAccount]] = {
    db.run(UserAccounts.filter(_.user_name === user_name).result.headOption)
  }

  def findByUserNameAndPassword(user_name: String, password: String): Future[Option[UserAccount]] = {
    db.run(UserAccounts.filter(x=> (x.user_name === user_name && x.password === password)).result.headOption)
  }

  private class UserAccountTable(tag: Tag) extends Table[UserAccount](tag, "config_user_account") {
    def id = column[Long]("id", O.PrimaryKey)
    def user_name = column[String]("user_name")
    def password = column[String]("password")

    private type UserAccountTupleType = (
      Long,
      String,
      String
    )
    //
    private val userAccountShapedValue = (id, user_name, password).shaped[UserAccountTupleType]
    //
    private val toModel: UserAccountTupleType => UserAccount = { userAccountTuple =>
      UserAccount(
        id = userAccountTuple._1,
        username = userAccountTuple._2,
        password = userAccountTuple._3
      )
    }
    private val toTuple: UserAccount => Option[UserAccountTupleType] = { userAccount =>
      Some {
        (
          userAccount.id,
          userAccount.username,
          userAccount.password
        )
      }
    }

    def * = userAccountShapedValue <> (toModel,toTuple)
  }
}