package listener

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import actors.Leadpath.LeadpathArrival
import dao.SessionDAO
import dao.RevenueDAO
import org.joda.time.{ DateTime, DateTimeZone }
import models.Revenue
import actors.Leadpath.LeadpathListing
import play.Logger
import javax.inject.Inject

class AggregatePriceListener @Inject() (val sessionDAO: SessionDAO, val revenueDAO: RevenueDAO) extends LeadpathEventListener {
  
    def processMessage(message: LeadpathArrival) : Future[Unit] = {
      var con_f = 0
      var total_revenue: BigDecimal = 0.0
      
      Logger.debug(s"Starting totalling")
      message.lead.listings.map( listings =>
        listings.foreach { listing : LeadpathListing =>
          listing.price.map { price =>
            con_f = con_f + 1
            total_revenue = total_revenue + price
          }
        }
      )
      Logger.info(s"Totalled the revenue: ${con_f}, ${total_revenue}")
      sessionDAO.touch(message.id.toLong)
      for {
        session <- sessionDAO.findBySessionId(message.id.toLong)
        revenue_obj_option <- session match {
          case Some(session) =>
            revenueDAO.findBySessionId(message.id.toLong)
          case None => {
            Logger.warn(s"Failed to find a session ${message.id}")
            Future.failed(new Exception("Failed to find a session " + message.id))
          }
        }
        temp <- revenue_obj_option match {
      	  case Some(revenue_obj) =>
            if (total_revenue > 0.0) {
              revenue_obj.total_revenue = total_revenue
              revenue_obj.con_f = con_f
              revenue_obj.updated_at = new DateTime(DateTimeZone.UTC)
            }

            for {
              ran <- revenueDAO.update(revenue_obj)
              revenue_obj <- revenueDAO.find(revenue_obj.id)
            } yield revenue_obj
          case _ =>
            if(total_revenue > 0.0) {
              revenueDAO.insert(
                Revenue(
                  0L,
                  message.id.toLong,
                  total_revenue,
                  con_f,
                  new DateTime(DateTimeZone.UTC),
                  new DateTime(DateTimeZone.UTC)
                )
              ).map(Option(_))
            }
            else {
              Future.successful(None)
            }
        }
      } yield Unit
    }
}