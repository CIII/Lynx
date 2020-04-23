package controllers

import abTest.{ABTestLoggingService, ABTestParticipant, ABTestService}
import api.SessionEvent
import com.google.inject.Inject
import dao._
import play.Environment
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ABTestController @Inject()(
  val abTestService: ABTestService,
  val abTestLoggingService: ABTestLoggingService,
  val attributeDAO: AttributeDAO,
  val domainDAO: DomainDAO,
  val eventAttributeDAO: EventAttributeDAO,
  val eventDAO: EventDAO,
  val eventTypeDAO: EventTypeDAO,
  val sessionDAO: SessionDAO,
  val sessionAttributeDAO: SessionAttributeDAO,
  implicit val configuration: Configuration,
  val environment: Environment,
  val urlDAO: UrlDAO
) extends Controller with SessionEvent with ABTestParticipant{

  val REQUIRED_PARAMS = "req_param"

  implicit val writes = new Writes[scala.collection.mutable.Map[String, Any]]{
    override def writes(m: scala.collection.mutable.Map[String, Any]): JsValue = {
      import scala.collection.JavaConversions._
      JsObject(
        m.map{case(key, value) =>
          key -> (value match {
            case x: String => JsString(x)
            case x: Int => JsNumber(x)
            case x: List[Any] => JsArray(
              x.map( y => y match {
                case z: String => JsString(z)
                case z: Int => JsNumber(z)
              })
            )
            case x: java.util.List[_] => JsArray(
              x.map( y => y match {
                case string: String => JsString(string)
                case int: Int => JsNumber(int)
                case double: Double => JsNumber(double)
              })
            )
            case _ =>
              Logger.debug(value.getClass.toString)
              JsString("ERROR")
          })
        }
      )
    }
  }

  def flush_tests = Action.async { implicit request =>

    val results = ABTestService.flush
    Future.successful(Ok("Flushing tests\n" + results.toString))
  }

  //TODO CANT ALWAYS CREATE SESSION ATTRIBUTE
  def get_params = Action.async {
    implicit request =>

      val (sid, ps_id) = utils.utilities.get_session_id_and_page_served_id(request)
      val event = ps_id match {
        case Some(event_id) =>
          Await.result(eventDAO.findByEventId(event_id),
            configuration.getInt("dbs.lookup.timeout.seconds").get.seconds)
        case None =>
          None
      }
      val domain_id = sid match {
        case Some(session_id) =>
          Await.result(sessionDAO.findBySessionId(session_id),
            configuration.getInt("dbs.lookup.timeout.seconds").get.seconds) match {
            case Some(s: models.Session) => s.domain_id
            case _ => None
          }
        case None =>
          None
      }

      val result =
        withABTestPrep(request.session, event.getOrElse(null), domain_id) { queryMap => { request =>

          request.queryString.get(REQUIRED_PARAMS) match {
            case Some(values) =>
              val testParams: scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, Any]] =
                abTestService.getParams(queryMap, values)
              val params = scala.collection.mutable.Map[String, Any]()
              testParams.size match {
                case 0 =>
                  NotFound("Specified parameters not being tested")
                case _ =>
                  testParams.foreach {
                    case (experiment, experiment_values) =>
                      experiment_values.foreach {
                    case (param, value) =>
                      if(!event.isEmpty){
                        abTestLoggingService.persist_test_value (value.toString, param, experiment, sid, event.get)
                      }
                      params += (param -> value)
                    }
                  }
                  Ok (Json.toJson (params) )
              }
            case None =>
              NotFound("No parameter specified")
          }
        }
        }
      Future.successful(result)
  }

}
