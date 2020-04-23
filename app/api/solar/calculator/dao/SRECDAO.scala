package api.solar.calculator.dao

import javax.inject.Inject

import api.solar.calculator.models.SREC
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class SRECDAO @Inject()(@NamedDatabase("solarcalculator") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val SRECs = TableQuery[SRECTable]

  def all(): Future[Seq[SREC]] = db.run(SRECs.result)

  def insert(srec: SREC): Future[SREC] = db.run(SRECs returning SRECs.map(_.state) into ((e, state) => e.copy(state = state)) += srec)

  def batchInsert(srecs: Seq[SREC]): Future[Unit] = db.run(SRECs ++= srecs).map { _ => () }

  def update(srec: SREC): Future[Unit] = {
    val formToUpdate = srec.copy(srec.state)
    db.run(SRECs.filter(_.state === srec.state).update(formToUpdate)).map(_ => ())
  }

  def find(state: String): Future[Option[SREC]] = {
    db.run(SRECs.filter(_.state === state).result.headOption)
  }

  private class SRECTable(tag: Tag) extends Table[SREC](tag, "SRECs") {
    def state = column[String]("state")
    def srec = column[Int]("srec")
    
    private type SRECTupleType = (
      String,
      Int
    )
    //
    private val formShapedValue = (state, srec).shaped[SRECTupleType]
    //
    private val toModel: SRECTupleType => SREC = { srecTuple =>
      SREC(
        state = srecTuple._1,
        srec = srecTuple._2
      )
    }
    private val toTuple: SREC => Option[SRECTupleType] = { srec =>
      Some {
        (
          srec.state,
          srec.srec
        )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}