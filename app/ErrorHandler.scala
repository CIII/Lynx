import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import play.api.Logger
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.http.Status
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.mvc.Results._
import controllers.routes
import play.api.http.DefaultHttpErrorHandler
import javax.inject.Singleton
import javax.inject.Inject
import play.api.Environment
import play.api.Configuration
import play.api.OptionalSourceMapper
import javax.inject.Provider
import play.api.routing.Router

@Singleton
class ErrorHandler @Inject()(
    val environment: Environment,
    val config: Configuration,
    val sourceMapper: OptionalSourceMapper,
    val router: Provider[Router]
    ) extends DefaultHttpErrorHandler(environment, config, sourceMapper, router) {
  
  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {
    if (statusCode == Status.NOT_FOUND) {
      Logger.error("404 Error - url: %s, referer: %s, querystring: %s, body: %s".format(
        request.uri,
        request.headers.get("referer").getOrElse("N/A"),
        request.rawQueryString,
        request.asInstanceOf[Request[AnyContent]].body.toString
      ))
    
      if(shouldRedirect(request.uri)){
        Future.successful(Redirect(routes.PageController.index_new()))
      } else {
        Logger.error("Asset 404 - Not Redirecting")
        Future.successful(BadRequest)
      }
    } else {
      super.onClientError(request, statusCode, message)
    }
  }
  
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
		Logger.error("500 Internal Server Error: %s".format(exception.getMessage))
    super.onServerError(request, exception)    
  }
  
  def shouldRedirect(url: String): Boolean = {
    // if url contains a dot and then characters, and does not contain a dot followed by htm,
    // then don't redirect.  otherwise do.
    if(url.matches(".{1,}\\..{1,}") && !(url.endsWith(".htm") || url.endsWith(".html") || url.endsWith(".js"))){
      return false;
    } else {
      return true;
    }
  }

}
