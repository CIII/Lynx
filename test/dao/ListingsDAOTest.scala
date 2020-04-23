package dao

import org.specs2.mutable.Specification
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import actors.Leadpath.LeadpathListing
import play.Logger
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ListingsDAOTest extends Specification {
  
  val config: Config = ConfigFactory.load("dev.conf")
  val configuration: Configuration = new Configuration(config)
  val injector = new GuiceApplicationBuilder()
      .loadConfig(configuration)
      .injector
  
  "The listings DAO" should {
    "retrieve a populated list" in {
      val name1 = "Test Order 1"
      val price1 = BigDecimal.decimal(1.0)
      val name2 = "Test Order 2"
      val price2 = BigDecimal.decimal(2.0)
      val dao: ListingsDAO = injector.instanceOf[ListingsDAO]
      val inputListings: Seq[LeadpathListing] = Seq[LeadpathListing](
          LeadpathListing(name1, Some(price1)),
          LeadpathListing(name2, Some(price2)))
      val populatedListingsFuture: Future[Seq[LeadpathListing]] = dao.populateListings(inputListings)
      val populatedListings = Await.result(populatedListingsFuture, Duration.Inf)
      

      "that have some things that" should {
        { populatedListings must not beNull }
        "be correct in" in {
          val first = populatedListings.head
          "the first item, which" should {
            { first.name must be equalTo(name1) }
            { first.price must beSome(price1) }
            { first.imageUrl must beSome("image url") }
            { first.siteUrl must beSome("site url") }
            { first.rating must beSome(1) }
            { first.reviewCount must beSome(3) }
            { first.score must beSome(5) }
            { first.descs.head must be equalTo("desc 1 1") }
            { first.descs.tail.head must be equalTo("desc 1 2") }
            { first.descs.tail.tail.head must be equalTo("desc 1 3") }
          }
          val second = populatedListings.tail.head
          "the second item, which" should {
            { second.name must be equalTo(name2) }
            { second.price must beSome(price2) }
            { second.imageUrl must beSome("image url 2") }
            { second.siteUrl must beSome("site url 2") }
            { second.rating must beSome(2) }
            { second.reviewCount must beSome(4) }
            { second.score must beSome(6) }
            { second.descs.head must be equalTo("desc 2 1") }
            { second.descs.tail.head must be equalTo("desc 2 2") }
            { second.descs.tail.tail.head must be equalTo("desc 2 3") }
          }
        }
      }
		}
  }
  
}