package controllers

import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

import api.TokenAuthentication
import dao._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc.Controller
import play.libs.Json
import slick.driver.JdbcProfile
import utils.utilities

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApiController @Inject()(
  implicit val apiTokenDAO: ApiTokenDAO,
  val ws: WSClient,
  val messagesApi: MessagesApi,
  implicit val environment: play.api.Environment,
  implicit val configuration: play.api.Configuration,
protected val dbConfigProvider: DatabaseConfigProvider
) extends Controller with I18nSupport with TokenAuthentication with HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  def ping = withApiToken {
    token => { request =>
      Ok("Valid Token found!")
    }
  }

  /*
    DIMENSIONS + KEY
    session_id, is_robot, TRAFFIC SOURCE ATTRIBUTES [traffic_source_id, campaignid, keyword ...], state, zipcode, property_ownership, electric_bill

    MEASURES
    steps* [count(events), grouped by event type], new_arrival(0/1)*, forms, conu, conf, revenue, secs* (on site time)

    bounce = session with 0 form steps and 0 form submits
    cold feet = form steps but no submit events
    conversion = submit events
    new_arrival = only session for browser_id
    on site time = min(created_at) on session - look for (page closed created_at)

    can be deferred to v2
  */

  def getRevenueData = withApiToken {
    token => { request =>
      val pastTime = Calendar.getInstance()
      pastTime.add(Calendar.DATE, -7)
      val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val startDateTime = request.getQueryString("startDate").getOrElse(date.format(pastTime.getTime))
      val endDateTime = request.getQueryString("endDate").getOrElse(date.format(Calendar.getInstance().getTime))
      Ok(Json.parse(s"""{"startDateTime": "$startDateTime", "endDate": "$endDateTime", "results": [${Await.result(db.run(sql"""
        SELECT JSON_OBJECT(
            'session_id', sessions.id,
            'is_robot', sessions.is_robot,
            'session_attributes', (
              SELECT
                CONCAT(
                  '[',
                  GROUP_CONCAT(JSON_OBJECT(attributes.name, session_attributes.value)),
                  ']'
                ) as json_value
                FROM session_attributes
                INNER JOIN attributes ON attributes.id = session_attributes.attribute_id
                WHERE session_attributes.session_id = sessions.id
            ),
            'forms', (
              SELECT CONCAT(
                '[',
                GROUP_CONCAT(
                  JSON_OBJECT(
                    'state', forms.state,
                    'zipcode', forms.zip,
                    'property_ownership', forms.property_ownership,
                    'electric_bill', forms.electric_bill
                  )
                ),
                ']'
              )
              FROM forms
              WHERE forms.session_id = sessions.id
            ),
            'event_counts', (
              SELECT
                CONCAT(
                  '[',
                  GROUP_CONCAT(
                    JSON_OBJECT(
                      event_types.name,
                      (
                        SELECT COUNT(*)
                        FROM events
                        WHERE event_type_id = event_types.id AND events.session_id = sessions.id
                      )
                    )
                  ),
                  ']'
                )
              FROM event_types
              WHERE (
                SELECT COUNT(*)
                FROM events
                WHERE event_type_id = event_types.id AND events.session_id = 42
              ) > 0
            ),
            'new_arrival', 0,
            'conf', revenues.con_f,
            'revenue', revenues.total_revenue,
            'secs', 0,
            'created_at', DATE_FORMAT(sessions.created_at, '%Y-%m-%d')
          ) AS json
          FROM sessions
          LEFT JOIN forms ON forms.session_id = sessions.id
          LEFT JOIN revenues ON revenues.session_id = sessions.id
          WHERE CAST(sessions.created_at as DATE) BETWEEN CAST('#$startDateTime' as DATE) AND CAST('#$endDateTime' as DATE)""".as[(String)]), Duration.Inf
      ).mkString(",")}]}""").toString)
    }
  }
}
