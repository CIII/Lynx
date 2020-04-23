package dao

import org.specs2.mutable.Specification
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.specs2.mock.Mockito
import slick.jdbc.JdbcDataSource
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.dbio.DBIOAction
import models.SessionAttribute

class SessionAttributesDAOTest extends Specification with Mockito {
  val config: Config = ConfigFactory.load("unit.conf")
  val configuration: Configuration = new Configuration(config)
  val injector = new GuiceApplicationBuilder()
      .loadConfig(configuration)
      .injector
      
  "The session attributes DAO" should {
    "retrieve multiple session attributes" in {
      val attributeIds = List(3L, 1L, 2L)
      val sessionId = 1L;
      val value1 = "Test value 1"
      val values = attributeIds.map { (sessionId, _, "Test value") }
      def sessionAttributeInsertAction(v: (Long, Long, String)) = sqlu"""INSERT INTO session_attributes VALUES (DEFAULT, ${v._1}, ${v._2}, ${v._3}, DEFAULT, DEFAULT)"""
      val configProvider = injector.instanceOf[DatabaseConfigProvider]
      val inserts = DBIO.sequence(values.map(sessionAttributeInsertAction))
      
      val dao: SessionAttributeDAO = injector.instanceOf[SessionAttributeDAO]
      val sessionAttributesFuture = for {
        insertResults <- configProvider.get.db.run(inserts)
        result <- dao.findBySessionIdAndAttributes(sessionId, List(1L, 3L))
      } yield result
      
      val sessionAttributes = Await.result(sessionAttributesFuture, Duration.Inf)
      println(sessionAttributes.toString)
      failure("Not implemented")
    }
  }

}