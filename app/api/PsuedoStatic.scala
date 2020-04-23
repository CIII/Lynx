package api

import play.api.mvc.{AnyContent, Request, Result, Results}

import scala.concurrent.Future

/**
  * Created by slin on 8/9/17.
  */
trait PsuedoStatic {

  val MAX_AGE = 604800;

  def withCacheControl(f: Request[AnyContent] => Result): (Request[AnyContent] => Future[Result]) = {
    implicit request =>
      Future.successful(f(request).withHeaders("Cache-Control" -> s"max-age=${MAX_AGE.toString}"))
  }
}
