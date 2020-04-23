package filters

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger

/**
  * Created by slin on 3/30/17.
  */

//TODO WE NEED TO RESTRICT DOMAINS
class MultiDomainSessionFilter @Inject() (
                                          implicit val playConfig: play.api.Configuration) extends EssentialFilter {
  def apply(next: EssentialAction) = EssentialAction { implicit request =>
    next(request).map { result =>

      play.api.mvc.Cookies.fromSetCookieHeader(result.header.headers.get("Set-Cookie")).get("PLAY_SESSION") match {
        case Some(sessionCookie: Cookie) =>

          val domain = utils.utilities.get_domain_name(true)
          Logger.debug(s"Setting cookie domain: $domain")
          val newSessionCookie = sessionCookie.copy(domain=Some(domain))
          result.withCookies(newSessionCookie)
        case _ =>
          result
      }
    }
  }
}
