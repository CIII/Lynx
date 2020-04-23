package api.solar.calculator.cache

import api.solar.calculator.dao._
import com.google.inject.Inject
import com.redis
import com.redis.{RedisClient, RedisClientPool}
import play.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by slin on 2/16/17.
  */
object SolarCalculatorCacheService {
  var pool: RedisClientPool = null;
}

class SolarCalculatorCacheService @Inject()(
  val utilityDAO: UtilityDAO,
  val costByStateDAO: CostByStateDAO,
  val costByZipDAO: CostByZipDAO,
  val solarYieldDAO: SolarYieldDAO,
  val srecDAO: SRECDAO,
  val systemCostDAO: SystemCostDAO,
  implicit val configuration: play.Configuration
){
  import SolarCalculatorCacheService._
  initializePool()

  private def initializePool(): Boolean = {
    if(SolarCalculatorCacheService.pool == null) {
      val host = configuration.getString("redis.host")
      val portString = configuration.getString("redis.port")
      val port = portString.toInt
      try {
        SolarCalculatorCacheService.pool = new RedisClientPool(
          host,
          port
        )
      } catch {
        case e: java.lang.RuntimeException => {
          val message: String = e.getMessage
          Logger.error(s"Failed to establish a connection to Redis for solar calculator. $message")
          return false
        }
      }
    }

    return true
  }

  //TODO REFACTOR OUT GET
  /**
    * Searches for utility specific cost/average, then state cost/average. Then default to mysql for similar values
    * @param state
    * @param utility_company
    * @return (cost, average usage)
    */
  def get_cost_and_average_usage(utility_company: String, state: String): Future[(Float, BigDecimal)] ={

    //12 hours
    val EXPIRATION = 43200L

    val standard_utility_company = utility_company.replaceAll(" ", "_")
    val cache_utility_cost_key = standard_utility_company + "_cost_per_kwh"
    val cache_utility_annual_usage_key = standard_utility_company + "_annual_usage"
    
    SolarCalculatorCacheService.pool.withClient { client =>
      (client.get(cache_utility_cost_key), client.get(cache_utility_annual_usage_key)) match {
        //Both are found return
        case (Some(cost_str), Some(usage_str)) =>
          Future((cost_str.toFloat, BigDecimal(usage_str)))

        //One or the other is not found, look based on state
        case (_, _) =>
          val cache_state_cost_key = state + "_cost_per_kwh"
          val cache_state_annual_usage_key = state + "_cost_per_kwh"

          (client.get(cache_state_cost_key), client.get(cache_state_annual_usage_key)) match {
            // Both are found, return
            case (Some(cost_str), Some(usage_str)) =>
              Future((cost_str.toFloat, BigDecimal(usage_str)))
            //One or the other is not found, look in database
            case (_, _) =>
              utilityDAO.findByStateAndUtilityCompany(state, utility_company).map {
                //Found, return but first persist
                case Some(utility) =>
                  set_key(cache_utility_cost_key, utility.cost, EXPIRATION)
                  set_key(cache_utility_annual_usage_key, utility.average_kwh_per_user, EXPIRATION)
                  (utility.cost, utility.average_kwh_per_user)
                case None =>
                  Await.result(
                    costByStateDAO.find(state).map {
                      case Some(state) =>

                        set_key(cache_state_cost_key, state.cost, EXPIRATION)
                        set_key(cache_state_annual_usage_key, state.average_kwh_per_user, EXPIRATION)
                        (state.cost, state.average_kwh_per_user)
                      case None =>
                        throw new IllegalArgumentException(s"Cost data does not exist for the state ${state} and utility ${utility_company}.")
                    },
                    Duration.Inf)
              }
          }

      }
    }

  }

  def get_cost_by_zipcode(zip_code: String): Future[Float] = {

    val EXPIRATION = 86400L

    val cache_cost_key = zip_code + "_cost_per_kwh"
    
    SolarCalculatorCacheService.pool.withClient { client =>

      client.get(cache_cost_key) match {
        case Some(zip) =>
          Future(zip.toFloat)
        case None =>
          costByZipDAO.find(zip_code).map {
            case Some(zip) =>
              set_key(cache_cost_key, zip.cost, EXPIRATION)
              zip.cost
            case _ =>
              throw new Exception(s"Did not find zip code ${zip_code}")
          }
      }
    }
  }

  /**
    * Get solar yield data
    * @param zip_code
    * @return
    */
  def get_solar_yield(zip_code: String): Future[List[Float]] = {

    val EXPIRATION = 86400L

    val cache_solar_yield_key = zip_code + "_solar_yields"

    SolarCalculatorCacheService.pool.withClient { client =>
      val list = client.lrange(cache_solar_yield_key, 0, -1).get
      list.size match {
        case 0 =>
          solarYieldDAO.find(zip_code).map {
            case Some(solar_yield) =>
              val yields = List(solar_yield.ac_monthly_1, solar_yield.ac_monthly_2, solar_yield.ac_monthly_3, solar_yield.ac_monthly_4,
                solar_yield.ac_monthly_6, solar_yield.ac_monthly_6, solar_yield.ac_monthly_7, solar_yield.ac_monthly_8,
                solar_yield.ac_monthly_9, solar_yield.ac_monthly_10, solar_yield.ac_monthly_11, solar_yield.ac_monthly_12)
              set_key(cache_solar_yield_key, yields, EXPIRATION)

              yields
            case None =>
              //TODO NEED TO ADD DEFAULT
              List(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
          }
        case _ =>
          Future(
            list.map(slr_yield => slr_yield.get.toFloat)
          )
      }
    }
  }

  /**
    *
    * @param state
    * @return
    */
  def get_SREC(state: String): Future[Double] = {

    val EXPIRATION = 86400L

    val cache_SREC_key = state + "_SREC"
    SolarCalculatorCacheService.pool.withClient { client =>
      client.get(cache_SREC_key) match {
        case Some(srec) =>
          Future(srec.toDouble)
        case None =>
          srecDAO.find(state).map {
            case Some(srec) =>
              set_key(state, srec.srec, EXPIRATION)
              srec.srec
            case None =>
              0.0
          }
      }
    }
  }

  def get_system_cost(state: String): Future[(Float, Float)] = {
    val EXPIRATION = 86400L

    val cache_low_key = state + "_system_cost_low"
    val cache_high_key = state + "_system_cost_high"

    SolarCalculatorCacheService.pool.withClient { client =>
      (client.get(cache_low_key), client.get(cache_high_key)) match {
        case (Some(low), Some(high)) =>
          Future(low.toFloat, high.toFloat)
        case (_, _) =>
          systemCostDAO.find(state).map {
            case None =>
              Await.result(
                systemCostDAO.average_costs.map {
                  averageLowHigh =>
                    set_key(cache_low_key, averageLowHigh._1, EXPIRATION)
                    set_key(cache_high_key, averageLowHigh._2, EXPIRATION)

                    averageLowHigh
                }, Duration.Inf)
            case Some(systemCost) =>
              set_key(cache_low_key, systemCost.low, EXPIRATION)
              set_key(cache_high_key, systemCost.high, EXPIRATION)
              (systemCost.low, systemCost.high)
          }
      }
    }
  }

  private def set_key(key: String, value: Any, exp_time: Long) = {

    SolarCalculatorCacheService.pool.withClient { client =>
      value match {
        case list: List[_] =>
          if (client.llen(key).get == 0) {
            list.foreach(value => client.lpush(key, value))
            client.expire(key, exp_time.toInt)
          }
        case _ =>
          try {
            client.set(key, value, false, redis.Seconds(exp_time))
          } catch {
            case e : Throwable => {
              Logger.warn(s"Error setting key ${key}, value ${value}", e)
              throw e
            }
          }
      }
    }
  }

}
