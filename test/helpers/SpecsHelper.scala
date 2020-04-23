package helpers

import java.util
import java.util.List

import com.typesafe.config._
import play.api.Logger

import scala.collection.JavaConversions._

object SpecsHelper{

  var additionalConfig: Option[Map[String,String]] = None

  //TODO refactor to reading from dev.conf or additional test conf that loads dev.conf
  def getAdditionalConfig: Map[String, String] = {
    //ConfigFactory.load("dev.conf").root.unwrapped.asScala.mapValues(_.toString).toMap
//    Map(
//      "slick.dbs.default.db.url" -> "jdbc:mysql://localhost/easiersolar_testing",
//      "slick.dbs.default.db.user" -> "root",
//      "slick.dbs.default.db.password" -> "",
//      "slick.dbs.default.db.connectionTimeout" -> "15s"
//    )
    additionalConfig match {
      case None => additionalConfig = Some(readConfigFile())
      case _ =>
    }

    return additionalConfig.get
  }

  def readConfigFile(): Map[String,String] =  {
    val config = ConfigFactory.load("dev.conf")
    var configMap = collection.mutable.Map[String, String]()

    val ignoreList = scala.collection.immutable.List("play.modules.enabled")


    for(entry <- config.entrySet) {

      val key = entry.getKey.toString
      if(!ignoreList.contains(key)) {

        entry.getValue.unwrapped match {
          case l: List[String] => {
            var count: Int = 0
            for (item <- entry.getValue.unwrapped.asInstanceOf[List[String]]) {
              configMap += ((key + "." + count) -> item)
              count += 1
            }
          }
          case _ => {
            configMap += (key -> entry.getValue.unwrapped.toString)
          }
        }
      }
    }
    return configMap.toMap
  }


  def main(args: Array[String]): Unit = {
    println("Hello, world!")
  }
}