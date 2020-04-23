package api.solar.calculator

import api.solar.calculator.cache.SolarCalculatorCacheService
import api.solar.calculator.dao._
import api.solar.calculator.models.Utility
import api.solar.calculator.models.output._
import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Json

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by slin on 2/8/17.
 */

object SolarCalculatorService {

  val ZIP_CODE = "zip_code"
  val STATE = "state"
  val ELECTRIC_COMPANY = "electric_company"
  val POWER_BILL = "power_bill"
  val KWH_PRICE = "kwh_price"
  val SYSTEM_CAPACITY = "system_capacity"
  val SYSTEM_COVERAGE = "system_coverage"
  val LOAN_RATE = "loan_rate"
  val LOAN_PERIOD = "loan_period"
  val MODULE_TYPE = "module_type"
  val ARRAY_TYPE = "array_type"
  val TILT = "tilt"
  val AZIMUTH = "azimuth"
  val ROOF_SHADE = "roof_shade"
  val FORM_PROGRESSION = "form_progression"

  val CALC_DEFAULTS = Map(
    ELECTRIC_COMPANY -> "",
    POWER_BILL -> 175.50,
    KWH_PRICE -> -1.0,
    SYSTEM_CAPACITY -> -1.0F,
    SYSTEM_COVERAGE -> 80.0,
    MODULE_TYPE -> 0,
    ARRAY_TYPE -> 1,
    TILT -> 28.0F,
    AZIMUTH -> 180.0F,
    ROOF_SHADE -> "no shade",
    LOAN_RATE -> 0.0299F,
    LOAN_PERIOD -> 10)
}

class SolarCalculatorService @Inject()(
  val utilityDAO: UtilityDAO,
  val costByStateDAO: CostByStateDAO,
  val costByZipDAO: CostByZipDAO,
  val solarYieldDAO: SolarYieldDAO,
  val systemCostDAO: SystemCostDAO,
  val srecDAO: SRECDAO,
  val solarCalculatorCacheService: SolarCalculatorCacheService
) {

  import SolarCalculatorService._

  val system_degradation = 0.0095
  val energy_inflation = 0.033
  val srec_value_trend = -0.02
  val PPA_discount = 0.2
  val PPA_escalation = 0.029

  /**
    * Calculate solar savings, main method of SolarCalculatorService
    *
    * @param inputs Map with values that corresponds to CALC_DEFAULTS. These will be optionally available
    *               based on leadpath controller
    * @return Solar savings as json string
    */
  def get_solar_savings(
                         inputs: scala.collection.mutable.Map[String, Any]
                       ): Future[SolarCalculatorOutput] = {

    val calc_values = scala.collection.mutable.Map[String, Any]() ++= CALC_DEFAULTS
    var form_progression = 0
    for ((key, value) <- inputs) {
      key match {
        case ELECTRIC_COMPANY => calc_values(key) = value.asInstanceOf[String]
        case STATE | ROOF_SHADE => calc_values(key) = value.asInstanceOf[String]
        case POWER_BILL | KWH_PRICE | SYSTEM_COVERAGE => calc_values(key) = value.asInstanceOf[Double]
        case SYSTEM_CAPACITY | TILT | AZIMUTH | LOAN_RATE => calc_values(key) = value.asInstanceOf[Float]
        case MODULE_TYPE | ARRAY_TYPE | LOAN_PERIOD => calc_values(key) = value.asInstanceOf[Int]
        case FORM_PROGRESSION => form_progression = value.asInstanceOf[Int]
        case _ =>
      }
    }

    var confidence = get_confidence(form_progression)

    if (inputs.get(ZIP_CODE).isEmpty) throw new SolarCalculatorException("Zip code is required for solar calculation")

    calc_values(ZIP_CODE) = inputs.get(ZIP_CODE).get.asInstanceOf[String]
    calc_values(STATE) = Await.result(get_state_for_zip(calc_values(ZIP_CODE).asInstanceOf[String]), Duration.Inf)

    val zip_code = calc_values(ZIP_CODE).asInstanceOf[String]
    val state = calc_values(STATE).asInstanceOf[String]
    val electric_company = calc_values(ELECTRIC_COMPANY).asInstanceOf[String]
    (for {
      cost_per_kilowatts <- cost_per_kilowatts(zip_code, electric_company, state)
      monthly_solar_yield <- solar_yield(zip_code)
      state_SREC <- srec(state)
      (system_cost_low, system_cost_high) <- estimate_system_cost(state)

    } yield (cost_per_kilowatts, monthly_solar_yield, state_SREC, system_cost_low, system_cost_high)).map {

      case (db_cost_per_kilowatts_hour, monthly_solar_yield, state_SREC, system_cost_low, system_cost_high) =>
        //check for input kwh_price
        val kwh_price = calc_values(KWH_PRICE).asInstanceOf[Double]
        val cost_per_kilowatts_hour =
          if (kwh_price != -1.0) kwh_price else db_cost_per_kilowatts_hour

        val power_bill = calc_values(POWER_BILL).asInstanceOf[Double]
        var annual_usage = power_bill / cost_per_kilowatts_hour * 12.0

        val system_capacity = calc_values(SYSTEM_CAPACITY).asInstanceOf[Float]
        val system_coverage = calc_values(SYSTEM_COVERAGE).asInstanceOf[Double]
        var system_size =
          if (system_capacity < 0) system_coverage / 100.0 * 6 * annual_usage / monthly_solar_yield.sum else system_capacity

        val loan_rate = calc_values(LOAN_RATE).asInstanceOf[Float]
        val loan_period = calc_values(LOAN_PERIOD).asInstanceOf[Int]
        var (estimated_total_cost_low, estimated_total_cost_high) = (system_cost_low * system_size * 1000, system_cost_high * system_size * 1000)
        val PMT_low = -PMT(estimated_total_cost_low, loan_rate / 12.0, loan_period * 12)
        val PMT_high = -PMT(estimated_total_cost_high, loan_rate / 12.0, loan_period * 12)

        val power_production = monthly_solar_yield.map(month => month * (system_size / 6.0))

        val year_one_annual_power_production = power_production.sum

        var twenty_five_year_production = estimate_twenty_five_year_production(year_one_annual_power_production)

        val twenty_five_year_payment_low = List.range(1, 26).map(payment =>
          if (payment <= 10) {
            PMT_low * 12
          } else {
            0
          })
        val twenty_five_year_payment_high = List.range(1, 26).map(payment =>
          if (payment <= 10) {
            PMT_high * 12
          } else {
            0
          })

        val twenty_five_year_SREC_income = twenty_five_year_production.zipWithIndex.map(year =>
          if (year._2 < 10) {
            year._1 / 1000.0 * state_SREC
          } else {
            0
          })
        var monthly_SREC_income = twenty_five_year_SREC_income(0) / 12.0 * -1.0

        var twenty_five_year_grid_equivalent = estimate_twenty_five_year_grid_equivalent(cost_per_kilowatts_hour, annual_usage)
        var twenty_five_year_grid_equivalent_cumulative = cumulative_calc(twenty_five_year_grid_equivalent)
        var twenty_five_year_total_grid_equivalent = twenty_five_year_grid_equivalent.sum

        val estimated_new_power_co_bill = twenty_five_year_grid_equivalent
          .zip(twenty_five_year_production).map { case i: (Double, Double) => (i._1 / annual_usage) * (annual_usage - i._2) }
        var monthly_cash_cost = (estimated_new_power_co_bill(0) - twenty_five_year_SREC_income(0)) / 12.0

        var monthly_extra_grid_power = estimated_new_power_co_bill(0) / 12.0

        val twenty_five_year_payment_average = twenty_five_year_payment_high.zip(twenty_five_year_payment_low).map { case (a, b) => (a + b) / 2 }
        var monthly_loan_system_payment = twenty_five_year_payment_average(0) / 12.0 * -1.0

        var twenty_five_year_loan_cost = (estimated_new_power_co_bill, twenty_five_year_SREC_income, twenty_five_year_payment_average)
          .zipped.toList.map { case (a, b, c) => a - b - c }
        var twenty_five_year_loan_cost_cumulative = cumulative_calc(twenty_five_year_loan_cost)
        var monthly_loan_cost = twenty_five_year_loan_cost(0) / 12.0

        var twenty_five_year_loan_savings = twenty_five_year_loan_cost
          .zip(twenty_five_year_grid_equivalent).map { case i: (Double, Double) => i._2 - i._1 }
        var twenty_five_year_loan_savings_cumulative = cumulative_calc(twenty_five_year_loan_savings)
        var twenty_five_year_total_loan_savings = twenty_five_year_loan_savings.sum
        var year_one_loan_savings_percentage = twenty_five_year_loan_savings(0) / twenty_five_year_grid_equivalent(0)

        var twenty_five_year_cash_cost = estimated_new_power_co_bill.zip(twenty_five_year_SREC_income).map { case (a, b) => a - b }
        twenty_five_year_cash_cost = twenty_five_year_cash_cost.updated(0, twenty_five_year_cash_cost(0) + (estimated_total_cost_high + estimated_total_cost_low) / 2.0)
        var twenty_five_year_cash_cost_cumulative = cumulative_calc(twenty_five_year_cash_cost)
        var twenty_five_year_cash_savings = twenty_five_year_grid_equivalent.zip(twenty_five_year_cash_cost).map { case (a, b) => a - b }
        var twenty_five_year_cash_savings_cumulative = cumulative_calc(twenty_five_year_cash_savings)
        var twenty_five_year_total_cash_savings = twenty_five_year_cash_savings.sum
        var year_one_cash_savings_percentage = twenty_five_year_cash_savings(0) / twenty_five_year_grid_equivalent(0)

        val twenty_five_year_ppa_payment = estimate_twenty_five_year_ppa_payment(cost_per_kilowatts_hour,
          twenty_five_year_production, annual_usage,
          twenty_five_year_grid_equivalent)
        var monthly_ppa_system_payment = twenty_five_year_ppa_payment(0) / 12.0

        var twenty_five_year_ppa_cost = estimate_twenty_five_year_ppa_cost(cost_per_kilowatts_hour,
          twenty_five_year_production, annual_usage,
          twenty_five_year_grid_equivalent)
        var twenty_five_year_ppa_cost_cumulative = cumulative_calc(twenty_five_year_ppa_cost)
        var monthly_ppa_cost = twenty_five_year_ppa_cost(0) / 12.0

        var monthly_loan_savings = power_bill - monthly_loan_cost
        var monthly_ppa_savings = power_bill - monthly_ppa_cost
        var monthly_cash_savings = power_bill - monthly_cash_cost

        var loan_effective_price =
          (twenty_five_year_total_grid_equivalent - twenty_five_year_total_loan_savings) / annual_usage / 25.0
        var ppa_effective_price = monthly_ppa_cost / (annual_usage / 12.0)
        var cash_effective_price =
          (twenty_five_year_total_grid_equivalent - twenty_five_year_total_cash_savings) / annual_usage / 25.0

        var twenty_five_year_ppa_savings = twenty_five_year_grid_equivalent.zip(twenty_five_year_ppa_cost).map { case (a, b) => a - b }
        var twenty_five_year_ppa_savings_cumulative = cumulative_calc(twenty_five_year_ppa_savings)
        val twenty_five_year_total_ppa_savings = twenty_five_year_ppa_savings.sum
        var year_one_ppa_savings_percentage = twenty_five_year_ppa_savings(0) / twenty_five_year_grid_equivalent(0)

        var loan_savings_percentage =
          1.0 - (twenty_five_year_total_grid_equivalent - twenty_five_year_total_loan_savings) / twenty_five_year_total_grid_equivalent
        var ppa_savings_percentage =
          1.0 - (twenty_five_year_total_grid_equivalent - twenty_five_year_total_ppa_savings) / twenty_five_year_total_grid_equivalent
        var cash_savings_percentage =
          1.0 - (twenty_five_year_total_grid_equivalent - twenty_five_year_total_cash_savings) / twenty_five_year_total_grid_equivalent

        system_size = round(system_size)
        annual_usage = round(annual_usage)
        twenty_five_year_production = round(twenty_five_year_production)
        twenty_five_year_cash_savings = round(twenty_five_year_cash_savings)
        twenty_five_year_cash_savings_cumulative = round(twenty_five_year_cash_savings_cumulative)
        twenty_five_year_cash_cost = round(twenty_five_year_cash_cost)
        twenty_five_year_cash_cost_cumulative = round(twenty_five_year_cash_cost_cumulative)
        year_one_cash_savings_percentage = round(year_one_cash_savings_percentage)
        monthly_cash_cost = round(monthly_cash_cost)
        monthly_SREC_income = round(monthly_SREC_income)
        monthly_extra_grid_power = round(monthly_extra_grid_power)
        monthly_cash_savings = round(monthly_cash_savings)
        estimated_total_cost_low = round(estimated_total_cost_low)
        cash_effective_price = round(cash_effective_price)
        cash_savings_percentage = round(cash_savings_percentage)

        twenty_five_year_loan_savings = round(twenty_five_year_loan_savings)
        twenty_five_year_loan_savings_cumulative = round(twenty_five_year_loan_savings_cumulative)
        twenty_five_year_loan_cost = round(twenty_five_year_loan_cost)
        twenty_five_year_loan_cost_cumulative = round(twenty_five_year_loan_cost_cumulative)
        year_one_loan_savings_percentage = round(year_one_loan_savings_percentage)
        monthly_loan_system_payment = round(monthly_loan_system_payment)
        monthly_loan_cost = round(monthly_loan_cost)
        monthly_loan_savings = round(monthly_loan_savings)
        loan_effective_price = round(loan_effective_price)
        loan_savings_percentage = round(loan_savings_percentage)

        twenty_five_year_ppa_savings = round(twenty_five_year_ppa_savings)
        twenty_five_year_ppa_savings_cumulative = round(twenty_five_year_ppa_savings_cumulative)
        twenty_five_year_ppa_cost = round(twenty_five_year_ppa_cost)
        twenty_five_year_ppa_cost_cumulative = round(twenty_five_year_ppa_cost_cumulative)
        year_one_ppa_savings_percentage = round(year_one_ppa_savings_percentage)
        monthly_ppa_system_payment = round(monthly_ppa_system_payment)
        monthly_ppa_cost = round(monthly_ppa_cost)
        monthly_ppa_savings = round(monthly_ppa_savings)
        ppa_effective_price = round(ppa_effective_price)
        ppa_savings_percentage = round(ppa_savings_percentage)

        twenty_five_year_grid_equivalent = round(twenty_five_year_grid_equivalent)
        twenty_five_year_total_grid_equivalent = round(twenty_five_year_total_grid_equivalent)
        twenty_five_year_grid_equivalent_cumulative = round(twenty_five_year_grid_equivalent_cumulative)

        confidence = round(confidence)
        val result = SolarCalculatorOutput(
          inputs = Inputs(
            address = "Address",
            power_bill = power_bill,
            kwh_usage = annual_usage,
            usage_offset = 100.0,
            array_type = calc_values(ARRAY_TYPE).asInstanceOf[Int],
            module_type = calc_values(MODULE_TYPE).asInstanceOf[Int],
            loan_period = loan_period,
            loan_interest = loan_rate
          ),
          versions = "1.0.0",
          location_info = LocationInfo(
            lat = BigDecimal(42.36666488647461),
            lon = BigDecimal(-71.0333328247070),
            elev = 5.0,
            tz = -5.0,
            location = zip_code,
            city = "",
            state = state
          ),
          outputs = Outputs(
            proposed_system = System(
              system_size = system_size,
              current_usage = annual_usage,
              proposed_output = twenty_five_year_production(0),
              current_bill = power_bill
            ),
            cash = SavingsVsCosts(
              savings = Some(twenty_five_year_cash_savings),
              savings_total = Some(twenty_five_year_total_cash_savings),
              savings_cumulative = Some(twenty_five_year_cash_savings_cumulative),
              year_one_savings_percent = Some(year_one_cash_savings_percentage),
              cost = twenty_five_year_cash_cost,
              cost_total = round(twenty_five_year_cash_cost.sum),
              cost_cumulative = twenty_five_year_cash_cost_cumulative,
              monthly_cost = Some(monthly_cash_cost),
              system_payment = Some(0.0),
              srec_credits = Some(monthly_SREC_income),
              extra_grid_power = Some(monthly_extra_grid_power),
              net_monthly_savings = Some(monthly_cash_savings),
              upfront_cost = Some(estimated_total_cost_low),
              effective_price = Some(cash_effective_price),
              savings_percentage = Some(cash_savings_percentage)
            ),
            ppa = SavingsVsCosts(
              savings = Some(twenty_five_year_ppa_savings),
              savings_total = Some(twenty_five_year_total_ppa_savings),
              savings_cumulative = Some(twenty_five_year_ppa_savings_cumulative),
              year_one_savings_percent = Some(year_one_ppa_savings_percentage),
              cost = twenty_five_year_ppa_cost,
              cost_total = round(twenty_five_year_ppa_cost.sum),
              cost_cumulative = twenty_five_year_ppa_cost_cumulative,
              monthly_cost = Some(monthly_ppa_cost),
              system_payment = Some(monthly_ppa_system_payment),
              srec_credits = Some(0.0),
              extra_grid_power = Some(monthly_extra_grid_power),
              net_monthly_savings = Some(monthly_ppa_savings),
              upfront_cost = Some(0.0),
              effective_price = Some(ppa_effective_price),
              savings_percentage = Some(ppa_savings_percentage)
            ),
            loan = SavingsVsCosts(
              savings = Some(twenty_five_year_loan_savings),
              savings_total = Some(twenty_five_year_total_loan_savings),
              savings_cumulative = Some(twenty_five_year_loan_savings_cumulative),
              year_one_savings_percent = Some(year_one_loan_savings_percentage),
              cost = twenty_five_year_loan_cost,
              cost_total = round(twenty_five_year_loan_cost.sum),
              cost_cumulative = twenty_five_year_loan_cost_cumulative,
              monthly_cost = Some(monthly_loan_cost),
              system_payment = Some(monthly_loan_system_payment),
              srec_credits = Some(monthly_SREC_income),
              extra_grid_power = Some(monthly_extra_grid_power),
              net_monthly_savings = Some(monthly_loan_savings),
              upfront_cost = Some(0.0),
              effective_price = Some(loan_effective_price),
              savings_percentage = Some(loan_savings_percentage)
            ),
            grid = SavingsVsCosts(
              cost = twenty_five_year_grid_equivalent,
              cost_total = twenty_five_year_total_grid_equivalent,
              cost_cumulative = twenty_five_year_grid_equivalent_cumulative
            )
          ),
          confidence = confidence
        )
        result
    }
  }

  private def round(double: Double): Double = {
    Math.round(double * 100.0) / 100.0
  }

  private def round(doubles: List[Double]): List[Double] = {
    doubles.map {
      double => Math.round(double * 100.0) / 100.0
    }
  }

  /**
    * Get cost per kilowatts based on zip code, if not available, through utility company, then through state
    *
    * @param zip_code
    * @param utility_company
    * @param state
    * @return
    */
  private def cost_per_kilowatts(zip_code: String = "", utility_company: String = "",
                                 state: String = ""): Future[Float] = {

    if (zip_code != "") {
      solarCalculatorCacheService.get_cost_by_zipcode(zip_code)
    } else if (utility_company != "" && state != "") {
      solarCalculatorCacheService.get_cost_and_average_usage(utility_company, state).map { tuple =>
        tuple._1
      }
    } else {
      Future(0.0F)
    }
  }

  private def get_state_for_zip(zip_code: String): Future[String] = {

    costByZipDAO.find(zip_code).map {
      cost => cost.get.state
    }
  }

  /**
    *
    * @param Pv present value
    * @param R  periodic interest rate
    * @param n  number of periods
    * @return Monthly payment
    */
  private def PMT(Pv: Double, R: Double, n: Int): Double = {
    (Pv * R) / (1 - Math.pow((1 + R), (-n)))
  }

  private def solar_yield(zip_code: String): Future[List[Float]] = {
    solarCalculatorCacheService.get_solar_yield(zip_code)
  }

  private def srec(state: String): Future[Double] = {
    solarCalculatorCacheService.get_SREC(state)
  }

  /**
    *
    * @return Tuple with low and high estimate per state
    */
  private def estimate_system_cost(state: String): Future[(Float, Float)] = {
    solarCalculatorCacheService.get_system_cost(state)
  }

  private def estimate_twenty_five_year_production(year_one: Double) = {

    def predict_production(productions: List[Double]): List[Double] =
      productions.size match {
        case 25 =>
          productions
        case _ =>
          predict_production(productions :+ (productions.last * (1 - system_degradation)))
      }

    predict_production(List(year_one))
  }

  private def estimate_twenty_five_year_grid_equivalent(energy_cost: Double, usage: Double) = {

    val grid_equivalent = List(usage * energy_cost)

    def predict_grid_equivalent(values: List[Double]): List[Double] =
      values.size match {
        case 25 =>
          values
        case t =>
          predict_grid_equivalent(values :+ ((values(t - 1) / usage * (1 + energy_inflation) * usage)))
      }

    predict_grid_equivalent(grid_equivalent)
  }

  private def estimate_twenty_five_year_ppa_payment(energy_cost: Double, productions: List[Double], usage: Double,
                                                    grid_equivalent: List[Double]): List[Double] = {

    productions.zipWithIndex.toList.map {
      case (production, year) =>

        ((1 - PPA_discount) * energy_cost) * Math.pow(1 + PPA_escalation, year) * production
    }
  }

  private def estimate_twenty_five_year_ppa_cost(energy_cost: Double, productions: List[Double], usage: Double,
                                                 grid_equivalent: List[Double]): List[Double] = {

    productions.zipWithIndex.toList.map {
      case (production, year) =>

        val y = {
          if (usage > production) {
            energy_cost * Math.pow(1 + energy_inflation, year) * (usage - production)
          } else {
            0
          }
        }

        ((1 - PPA_discount) * energy_cost) * Math.pow(1 + PPA_escalation, year) * production + y
    }
  }

  private def cumulative_calc(list: List[Double]): List[Double] = {

    val cumulativeList = new ListBuffer[Double]()
    cumulativeList += list(0)

    def calc(index: Int, last: Double, cumulativeList: ListBuffer[Double]): ListBuffer[Double] = {

      val limit = list.size - 1
      index match {
        case `limit` => cumulativeList += (last + list(index))
          cumulativeList
        case _ =>
          cumulativeList += (last + list(index))
          calc(index + 1, last + list(index), cumulativeList)
      }
    }
    calc(1, list(0), cumulativeList)
    cumulativeList.toList
  }
  

  private def get_confidence(formProgression: Int): Double = {
    var confidence = 50.0
    val maxConfidence = 95.0
    val maxProgression = 8.0
    val confidenceIncrement = (maxConfidence - confidence)/maxProgression
    confidence += confidenceIncrement * (formProgression)
    confidence += Math.random() * 5.0
    if(confidence > maxConfidence) confidence = maxConfidence
    confidence = Math.round(confidence*100.0)/100.0
    confidence/100.0
  }
}

class SolarPanelModuleType {

  sealed abstract class Type(
      val type_val: Int,
      val name: String) {

    def ==(that: Type) = {
      (this.type_val - that.type_val) == 0
    }
  }

  case object STANDARD extends Type(1, "STANDARD")
  case object PREMIUM extends Type(2, "PREMIUM")
  case object THIN_FILM extends Type(3, "THIN_FILM")
  case object INVALID extends Type(-1, "INVALID")

  def get_type(type_val: Int) =
    type_val match {
      case 1 => STANDARD
      case 2 => PREMIUM
      case 3 => THIN_FILM
      case _ => INVALID
    }
}

class ArrayType {

  sealed abstract class Type(
      val type_val: Int,
      val name: String) {

    def ==(that: Type) = {
      (this.type_val - that.type_val) == 0
    }
  }

  case object FIXED_OPEN_RACK extends Type(0, "Fixed - Open Rack")
  case object FIXED_ROOF_MOUNTED extends Type(1, "Fixed - Roof Mounted")
  case object ONE_AXIS extends Type(2, "1-Axis")
  case object ONE_AXIS_BACKTRACKING extends Type(3, "1-Axis Backtracking")
  case object TWO_AXIS extends Type(4, "2-Axis")
  case object INVALID extends Type(-1, "INVALID")

  def get_type(type_val: Int) =
    type_val match {
      case 1 => FIXED_OPEN_RACK
      case 2 => FIXED_ROOF_MOUNTED
      case 3 => ONE_AXIS
      case 4 => ONE_AXIS_BACKTRACKING
      case 5 => TWO_AXIS
    }
}

case class SolarCalculatorException(error: String) extends Exception(error)
