package utils

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import com.tapquality.dao.MandrillDAO

@RunWith(classOf[JUnitRunner])
class EmailCommandTest extends Specification {
  
  val config: Config = ConfigFactory.load("dev.conf")
  val configuration: Configuration = new Configuration(config)
  def applicationBuilder = new GuiceApplicationBuilder()
    .loadConfig(configuration)
    .build();
  
  "The command" should {
    "send an email" in {
      val data = EmailCommandData(
          "jon@tapquality.com",
          "Jon",
          "mouse",
          Some("My City"),
          "electric bill",
          "current annual usage",
          "proposed system size",
          "loan total savings",
          "loan monthly cost",
          "ppa monthly cost",
          "cash monthly cost",
          "loan system payment",
          "ppa system payment",
          "cash system payment",
          "loan srec income",
          "ppa srec income",
          "cash srec income",
          "loan new power bill",
          "ppa new power bill",
          "cash new power bill",
          "loan monthly savings",
          "ppa monthly savings",
          "cash monthly savings",
          "loan y1 savings",
          "ppa y1 savings",
          "cash y1 savings",
          "loan lt savings",
          "ppa lt savings",
          "cash lt savings",
          "loan upfront cost",
          "ppa upfront cost",
          "cash upfront cost",
          "loan new ppkwh",
          "ppa new ppkwh",
          "cash new ppkwh",
          "loan savings pct",
          "ppa savings pct",
          "cash savings pct")
      val dao = applicationBuilder.injector.instanceOf(classOf[MandrillDAO])
      val command = new EmailCommand(dao)
      command.sendEmail(data, "homesolar")
      success("Success")
    }
  }
  
}