package dao

import org.specs2._
import play.api.inject.guice.GuiceApplicationBuilder
import java.io.File
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import javax.inject.Singleton
import play.Logger
import play.api.Mode
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import play.api.Configuration
import play.inject.Injector
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class PowerProvidersDAOTestIntegration extends Specification { def is = s2"""
  This is a performance test for the file vs. Redis power retrieval.
    Did run:                                $e1
  """
    
  var dao: PowerProvidersDAO = null
  
  val config: Config = ConfigFactory.load("dev.conf")
  val configuration: Configuration = new Configuration(config)
  val injector = new GuiceApplicationBuilder()
      .loadConfig(configuration)
      .injector
  dao = injector.instanceOf[PowerProvidersDAO]

    
  val e1 = {
    var i = 0;
    //for (i <- 0 to 1000) {
      val suppliersList = dao.getProviders("MA")
      val suppliersListJson = Json.toJson(suppliersList)
    //}
    1 must be
  }
}