/**
 *
 */
package security

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import com.google.inject.Inject
import play.api.Logger
import play.api.mvc.Results.Forbidden
import play.api.mvc._
import security.dao.UserAccountDAO
import security.models.UserAccount

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._


class ConfigUIDeadboltHandler @Inject()(
  val userAccountDAO: UserAccountDAO) extends DeadboltHandler{

  def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future(None)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = Future(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[UserAccount]] = {
    request.session.get(Security.username) match {
      case Some(username) =>
        userAccountDAO.findByUserName(username)
      case None =>
        Future(None)
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] =
    Future(Results.Redirect(controllers.config_ui.routes.ConfigurationUIController.login).withSession(request.session + ("redirect" -> request.path)))
}