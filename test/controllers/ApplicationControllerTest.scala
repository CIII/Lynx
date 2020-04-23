package controllers

import controllers._
import org.specs2.mutable._
import play.api.test.WithApplication
import play.api.test._
import play.api.test.Helpers._
import com.typesafe.config._
import helpers.SpecsHelper
import play.api.Logger


import scala.collection.JavaConverters._

class ApplicationControllerTest extends Specification {

  var fakeApplication: FakeApplication = _

  trait Context extends BeforeAfter {
    def before(): Unit = {
      fakeApplication = FakeApplication(additionalConfiguration = SpecsHelper.getAdditionalConfig)
    }

    def after(): Unit = {

    }
  }

  var applicationController = new controllers.ApplicationController(null, null, null, null, null, null, null, null, null)

  "validate_phone" should {
    "respond with an OK for a real number" in new Context{
      running(fakeApplication) {
        val result = applicationController.validate_phone("9499499494")(FakeRequest())
        status(result) must equalTo(OK)
      }
    }
    "respond with a Forbidden for a number with none digits" in new Context{
      running(fakeApplication) {
        val result = applicationController.validate_phone("Nine499499494")(FakeRequest())
        status(result) must equalTo(FORBIDDEN)
      }
    }
    "respond with a Forbidden for a number with wrong number of digits" in new Context{
      running(fakeApplication) {
        val result = applicationController.validate_phone("94994994949")(FakeRequest())
        status(result) must equalTo(FORBIDDEN)
      }
    }
  }
  "Router for validate phone" should {
    "respond with an OK for a real number" in new Context{
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/validate_phone/9499499494" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
        }
      }
    }
    "respond with a Forbidden for a number with none digits" in new Context{
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/validate_phone/Nine499499494" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(FORBIDDEN)
        }
      }
    }
    "respond with a Forbidden for a number with wrong number of digits" in new Context{
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/validate_phone/94994994949" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(FORBIDDEN)
        }
      }
    }
  }

  "browser_id" should {
    "respond with an OK with proper headers" in new Context {
      running(fakeApplication) {
        val result = applicationController.browser_id()(FakeRequest())
        status(result) must equalTo(OK)
        header(CONTENT_TYPE, result).get must equalTo("text/javascript")
      }
    }
  }

  "Router for browser_id" should {
    "respond with an OK with proper headers" in new Context {
      running(fakeApplication) {
        val routes: Map[String, String] = Map(
          "/browser_id.js" -> GET
        )
        routes.keys.foreach { r =>
          var Some(result) = route(fakeApplication, FakeRequest(routes(r), r))
          status(result) must equalTo(OK)
          header(CONTENT_TYPE, result).get must equalTo("text/javascript")
        }
      }
    }
  }
}