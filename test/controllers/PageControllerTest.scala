package controllers

import controllers._
import org.specs2.mutable._
import play.api.test.WithApplication
import play.api.test._
import play.api.test.Helpers._
import com.typesafe.config._
import helpers.SpecsHelper

import scala.collection.JavaConverters._

class PageControllerTest extends Specification {

  var fakeApplication: FakeApplication = _

  trait Context extends BeforeAfter {
    def before(): Unit = {
      fakeApplication = FakeApplication(additionalConfiguration = SpecsHelper.getAdditionalConfig)
    }

    def after(): Unit = {

    }
  }

  var pageController = new controllers.PageController(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
  "PageController" should {
    "respond with an OK for index" in new Context {
      running(fakeApplication) {
        val result = pageController.index("www.easiersolar.com")(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for editorial" in new Context {
      running(fakeApplication) {
        val result = pageController.editorial()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for getQuotes" in new Context {
      running(fakeApplication) {
        val result = pageController.getQuotes()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for compare" in new Context {
      running(fakeApplication) {
        var result = pageController.compare(160801)(FakeRequest())
        status(result) must equalTo(OK)
        result = pageController.compare(160802)(FakeRequest())
        status(result) must equalTo(OK)
        result = pageController.compare(999999)(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for thank_you" in new Context {
      running(fakeApplication) {
        val result = pageController.thank_you()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for installers" in new Context {
      running(fakeApplication) {
        val result = pageController.installers()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for privacy_policy" in new Context {
      running(fakeApplication) {
        val result = pageController.privacy_policy()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for terms_of_use" in new Context {
      running(fakeApplication) {
        val result = pageController.terms_of_use()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with an OK for middle_class_solar_incentive" in new Context {
      running(fakeApplication) {
        val result = pageController.middle_class_solar_incentive()(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
  }

  "Router" should {
    "respond with an OK for index" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/" -> GET,
          "/index" -> GET,
          "/index/" -> GET,
          "/index.html" -> GET,
          "/index.htm" -> GET,
          "/index.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for editorial" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/editorial" -> GET,
          "/editorial/" -> GET,
          "/editorial/index.html" -> GET,
          "/editorial/index.htm" -> GET,
          "/editorial/index.php" -> GET,
          "/editorial.html" -> GET,
          "/editorial.htm" -> GET,
          "/editorial.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for getQuotes" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/get-quotes" -> GET,
          "/get-quotes/" -> GET,
          "/get-quotes.html" -> GET,
          "/get-quotes.htm" -> GET,
          "/get-quotes.php" -> GET,
          "/get-quotes/index.html" -> GET,
          "/get-quotes/index.htm" -> GET,
          "/get-quotes/index.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for compare" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/compare/160801" -> GET,
          "/compare/160801/" -> GET,
          "/compare/160802" -> GET,
          "/compare/160802/" -> GET,
          "/compare/160803" -> GET,
          "/compare/160803/" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for thank_you" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/get-quotes/thanks" -> GET,
          "/get-quotes/thanks/" -> GET,
          "/get-quotes/thanks/index.html" -> GET,
          "/get-quotes/thanks/index.htm" -> GET,
          "/get-quotes/thanks/index.php" -> GET,
          "/get-quotes/thanks.html" -> GET,
          "/get-quotes/thanks.htm" -> GET,
          "/get-quotes/thanks.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for installers" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/installers" -> GET,
          "/installers/" -> GET,
          "/installers/index.html" -> GET,
          "/installers/index.htm" -> GET,
          "/installers/index.php" -> GET,
          "/installers.html" -> GET,
          "/installers.htm" -> GET,
          "/installers.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for privacy_policy" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/privacy-policy" -> GET,
          "/privacy-policy/" -> GET,
          "/privacy-policy/index.html" -> GET,
          "/privacy-policy/index.htm" -> GET,
          "/privacy-policy/index.php" -> GET,
          "/privacy-policy.html" -> GET,
          "/privacy-policy.htm" -> GET,
          "/privacy-policy.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for terms_of_use" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/terms-of-use" -> GET,
          "/terms-of-use/" -> GET,
          "/terms-of-use/index.html" -> GET,
          "/terms-of-use/index.htm" -> GET,
          "/terms-of-use/index.php" -> GET,
          "/terms-of-use.html" -> GET,
          "/terms-of-use.htm" -> GET,
          "/terms-of-use.php" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with an OK for middle_class_solar_incentive" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/blog/middle-class-solar-incentive" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
  }
}