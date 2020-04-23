package filters

import akka.stream.Materializer
import com.google.inject.Inject
import dao.SessionDAO
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future

/**
  * Created by slin on 3/30/17.
  */
class SessionFilter @Inject()(
   val sessionDAO: SessionDAO,
   implicit val mat: Materializer
)extends Filter{

  def apply(nextFilter: RequestHeader => Future[Result])
           (request: RequestHeader): Future[Result] = {

    request.session.get(utils.utilities.SESSION_ID) match {
      case Some(sid) =>
        //Assumes that sid should be long
        sessionDAO.touch(sid.toLong)
      case None =>
    }

    nextFilter(request)
  }
}
