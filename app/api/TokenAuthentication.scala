package api

import javax.inject.Inject

import dao.ApiTokenDAO
import play.api.mvc._
import models.ApiToken
import play.Logger

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.annotation.meta.getter

trait TokenAuthentication {
  self: Controller =>

    @(Inject @getter) val apiTokenDAO: ApiTokenDAO

    def extractToken(authHeader: String): Option[String] = {
      authHeader.split("Token token=") match {
        case Array(_, token) => Some(token)
        case _ => None
      }
    }

    /**
      * Fetch an API token from the request headers
      *
      * If one exists then allow the request else deny it
      *
      * curl -i https://easiersolar.com/api/[ROUTE] -H "Authorization: Token token=[TOKEN]"
      * curl -i https://easiersolar.com/api/[ROUTE]?token=[TOKEN]"
      */
    def withApiToken(f: => ApiToken => Request[AnyContent] => Result) = Action { implicit request =>
      if (request.method.toLowerCase == "get") {
        request.getQueryString("token") match {
          case Some(token) =>
            Await.result(apiTokenDAO.findByToken(token), Duration.Inf) match {
              case Some(token_obj) if token_obj.active => f(token_obj)(request)
              case _ => Unauthorized("Invalid API token")
            }
          case None => Unauthorized("Invalid API token")
        }
      } else {
        Logger.debug("Got an authenticatable POST")
        request.headers.get("Authorization") match {
          case Some(authHeaderToken) =>
            Logger.debug("Got an authorization header")
            extractToken(authHeaderToken) match {
              case Some(token) =>
                Logger.debug("Got a token")
                Await.result(apiTokenDAO.findByToken(token), Duration.Inf) match {
                  case Some(token_obj) if token_obj.active => f(token_obj)(request)
                  case _ => { Logger.debug("Unauthorized"); Unauthorized("Invalid API token") }
                }
              case None => { Logger.debug("Invalid token"); Unauthorized("Invalid API token") }
            }
          case None => { Logger.debug("Missing header"); Unauthorized("Missing Authorization") }
        }
      }
    }
}
