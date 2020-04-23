package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/** Provides a dummy endpoint that took the place of the post endpoint in [[LeadPathController]] for testing purposes.
 *  It is out of date and the return value is likely incompatible with the current functionality. 
 */
class PostDummy extends Controller {
  
  def postEndpoint() = Action.async { implicit request =>
    Future(Ok("{\"lead_id\": \"1234\" }"))
  }
}
