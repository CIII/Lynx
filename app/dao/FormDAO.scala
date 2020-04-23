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

class FormDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Forms = TableQuery[FormsTable]

  def all(): Future[Seq[models.Form]] = db.run(Forms.result)

  //def insert(form: Form): Future[Unit] = db.run(Forms += form).map { _ => () }

  def insert(form: models.Form): Future[models.Form] = db.run(Forms returning Forms.map(_.id) into ((f, id) => f.copy(id = id)) += form)

  def batchInsert(forms: Seq[models.Form]): Future[Unit] = db.run(Forms ++= forms).map { _ => () }

  def update(form: models.Form): Future[Unit] = {
    val formToUpdate = form.copy(form.id)
    db.run(Forms.filter(_.id === form.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(Forms.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[models.Form]] = {
    db.run(Forms.filter(_.id.get === id).result.headOption)
  }

  def findByFormId(form_id: Long): Future[Option[models.Form]] = {
    db.run(Forms.filter(_.id.get === form_id).result.headOption)
  }

  def findAllBySessionId(session_id: Long): Future[Seq[models.Form]] = {
    db.run(Forms.filter(_.session_id.getOrElse(0L) === session_id).result)
  }

  private class FormsTable(tag: Tag) extends Table[models.Form](tag, "forms") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def session_id = column[Option[Long]]("session_id")
    def event_id = column[Option[Long]]("event_id")
    def first_name = column[Option[String]]("first_name")
    def last_name = column[Option[String]]("last_name")
    def full_name = column[Option[String]]("full_name")
    def email = column[String]("email")
    def street = column[Option[String]]("street")
    def city = column[Option[String]]("city")
    def state = column[Option[String]]("state")
    def zip = column[Option[String]]("zip")
    def property_ownership = column[Option[String]]("property_ownership")
    def electric_bill = column[Option[String]]("electric_bill")
    def electric_company = column[Option[String]]("electric_company")
    def phone_home = column[Option[String]]("phone_home")
    def leadid_token = column[Option[String]]("leadid_token")
    def domtok = column[Option[String]]("domtok")
    def ref = column[Option[String]]("ref")
    def xxTrustedFormCertUrl = column[Option[String]]("xxTrustedFormCertUrl")
    def xxTrustedFormToken = column[Option[String]]("xxTrustedFormToken")
    def dob = column[Option[DateTime]]("dob")
    def post_status = column[Option[Int]]("post_status")
    def created_at = column[Option[DateTime]]("created_at")
    def updated_at = column[Option[DateTime]]("updated_at")

    private type FormTupleType = (
      Option[Long],
      Option[Long],
      Option[Long],
      Option[String],
      Option[String],
      Option[String],
      String,
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[DateTime],
      Option[Int]
    )
    //
    private val formShapedValue = (id, session_id, event_id,
      first_name, last_name, full_name, email, street, city, state, zip, property_ownership, electric_bill, electric_company, phone_home,
      leadid_token, domtok, ref, xxTrustedFormCertUrl, xxTrustedFormToken, dob, post_status
      ).shaped[FormTupleType]
    //
    private val toModel: FormTupleType => models.Form = { formTuple =>
      models.Form(
        id = formTuple._1,
        session_id = formTuple._2,
        event_id = formTuple._3,
        first_name = formTuple._4,
        last_name = formTuple._5,
        full_name = formTuple._6,
        email = formTuple._7,
        street = formTuple._8,
        city = formTuple._9,
        state = formTuple._10,
        zip = formTuple._11,
        property_ownership = formTuple._12,
        electric_bill = formTuple._13,
        electric_company = formTuple._14,
        phone_home = formTuple._15,
        leadid_token = formTuple._16,
        domtok = formTuple._17,
        ref = formTuple._18,
        xxTrustedFormToken = formTuple._19,
        xxTrustedFormCertUrl = formTuple._20,
        dob = formTuple._21,
        post_status = formTuple._22
      )
    }
    private val toTuple: models.Form => Option[FormTupleType] = { form =>
      Some {
        (
          form.id,
          form.session_id,
          form.event_id,
          form.first_name,
          form.last_name,
          form.full_name,
          form.email,
          form.street,
          form.city,
          form.state,
          form.zip,
          form.property_ownership,
          form.electric_bill,
          form.electric_company,
          form.phone_home,
          form.leadid_token,
          form.domtok,
          form.ref,
          form.xxTrustedFormCertUrl,
          form.xxTrustedFormToken,
          form.dob,
          form.post_status
          )
      }
    }

    def * = formShapedValue <> (toModel,toTuple)
  }
}