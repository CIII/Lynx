package dao

import play.api.db.slick.HasDatabaseConfigProvider
import play.api.db.slick.DatabaseConfigProvider
import javax.inject.Inject
import slick.driver.JdbcProfile
import slick.jdbc.{GetResult => GR}
import actors.Leadpath.LeadpathListing
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.ExecutionContext
import scala.collection.mutable.MapBuilder
import scala.concurrent.Promise
import play.Logger

case class Listing(id: Option[Long], name: String, imageUrl: Option[String], siteUrl: Option[String], rating: Option[BigDecimal], reviewCount: Option[Int], score: Option[Int])
case class ListingDesc(id: Option[Long], listingId: Long, desc: String)

class ListingsDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._
 
  private val Listings = TableQuery[ListingsTable]
  private val ListingDescs = TableQuery[ListingDescsTable]

  def populateListings(listings: Seq[LeadpathListing]) : Future[Seq[LeadpathListing]] = {
    var listingMap = Map.empty[String, LeadpathListing]
    for(listing <- listings) {
      listingMap += (listing.name -> listing)
    }
    val q = for {
      listing <- Listings
      if listing.name inSetBind(listingMap.keySet)
    } yield(listing)
    
    val descQuery = for {
      joinRow <- Listings.join(ListingDescs) on (_.id === _.listingId)
      if joinRow._1.name inSetBind(listingMap.keySet)
    } yield(joinRow._1, joinRow._2)
    
    val test = for {
      descriptions <- db.run(descQuery.result)
      descriptionsGrouped <- Future { descriptions.map(_._2).groupBy { x => x.listingId } }
      listings <- db.run(q.result)
      transformedListings <- {
        Future {
          for {
            listing <- listings
            oldListing <- listingMap.get(listing.name)
            newListing <- Option {
              val descriptions = descriptionsGrouped.getOrElse(listing.id.get, Seq.empty)
              LeadpathListing(oldListing.name, oldListing.price, listing.imageUrl, listing.siteUrl, listing.rating, listing.reviewCount, listing.score, descriptions.map(_.desc))
            }
          } yield (newListing)
        }
      }
    } yield(transformedListings)
    
    Logger.error(test.toString())

    test
  }
  
  private class ListingsTable(tag: Tag) extends Table[Listing](tag, "listings") {
    def * = (id, name, imageUrl, siteUrl, rating, reviewCount, score) <> (Listing.tupled, Listing.unapply)
    def ? = (Rep.Some(id), name, Rep.Some(imageUrl), Rep.Some(siteUrl), Rep.Some(rating), Rep.Some(reviewCount), Rep.Some(score)).shaped.<>({r=> import r._; _1.map(_=> Listing.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))
    
    val id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    val name = column[String]("name", O.Length(100, varying=true))
    val imageUrl = column[Option[String]]("image_url", O.Length(255, varying=true), O.Default(None))
    val siteUrl = column[Option[String]]("site_url", O.Length(255, varying=true), O.Default(None))
    val rating = column[Option[BigDecimal]]("rating", O.Default(None))
    val reviewCount = column[Option[Int]]("review_count", O.Default(None))
    val score = column[Option[Int]]("score", O.Default(None))
    
  }
  
  implicit def GetResultAttribute(implicit e0: GR[Option[Int]], e1: GR[String], e2: GR[Option[String]], e3: GR[Option[BigDecimal]]) = GR{
    prs => import prs._
    Listing.tupled((<<?[Long], <<[String], <<?[String], <<?[String], <<?[BigDecimal], <<?[Int], <<?[Int]))
  }
  
  private class ListingDescsTable(tag: Tag) extends Table[ListingDesc](tag, "listing_descs") {
    def * = (id, listingId, desc) <> (ListingDesc.tupled, ListingDesc.unapply)
    def ? = (Rep.Some(id), listingId, desc).shaped.<>({r=> import r._; _1.map(_=> ListingDesc.tupled((_1.get, _2, _3)))}, (_:Any) => throw new Exception("Inserting int ? projection not supported."))
    
    val id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    val listingId = column[Long]("listing_id")
    val desc = column[String]("desc")
  }
  
  implicit def GetResultAttribute(implicit e0: GR[Option[Int]], e1: GR[Int], e2: GR[String]) = GR{
    prs => import prs._
    ListingDesc.tupled((<<?[Long], <<[Int], <<[String]))
  }
}