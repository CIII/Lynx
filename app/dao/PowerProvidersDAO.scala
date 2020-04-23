package dao

import javax.inject.{Inject, Provider, Singleton}

import play.Configuration
import play.api.libs.json._
import play.Logger
import javax.inject.Inject

import com.redis.{RedisClient, RedisClientPool}
import utils.utilities._


object PowerProvidersDAO {

  var pool: RedisClientPool = null;
}

class PowerProvidersDAO @Inject() (configuration: Configuration) {
  import PowerProvidersDAO._
  initializePool()

  private def initializePool(): Boolean = {
    if(PowerProvidersDAO.pool == null) {
      val host = configuration.getString("redis.host")
      val portString = configuration.getString("redis.port")
      val port = portString.toInt
      try {
        PowerProvidersDAO.pool = new RedisClientPool(
          host,
          port
        )
      } catch {
        case e: java.lang.RuntimeException => {
          val message: String = e.getMessage
          Logger.error(s"Failed to establish a connection to Redis to retrieve power providers. $message")
          return false
        }
      }
    }
    
    return true
  }
  
  def getProviders(state: String): Option[List[Option[String]]]= {
    val listKey: String = "power_suppliers:" + state;

    try {
      PowerProvidersDAO.pool.withClient { client =>
        Logger.debug(client.hashCode().toString)
        client.lrange(listKey, 0, -1)
      }
    }catch{
      case _ : Throwable =>
        return Option(List.empty[Option[String]])
    }
    
  }

  def bestGuessZipProvider(zip: String): Option[String] = {
    val zipKey: String = s"best_guess_supplier:${zip}"

    try {
      PowerProvidersDAO.pool.withClient( client =>
        client.get(zipKey)
      )
    }catch{
      case _ : Throwable => None
    }
  }

  def cacheBestGuessZipProvider(zip: String, powerCompany: String) = {
    val zipKey: String = s"best_guess_supplier:${zip}"

    try {
      PowerProvidersDAO.pool.withClient( client =>
        client.set(zipKey,powerCompany)
      )
    }catch{
      case _ : Throwable => None
    }
  }
}