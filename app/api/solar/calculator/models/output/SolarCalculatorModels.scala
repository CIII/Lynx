package api.solar.calculator.models.output

import play.api.libs.json.{Json, Writes}

/**
  * Created by slin on 2/8/17.
  */

case class Inputs(
  var address: String,
  var power_bill: Double,
  var kwh_usage: Double,
  var usage_offset: Double,
  var array_type: Int,
  var module_type: Int,
  var loan_period: Int,
  var loan_interest: Double
)

object Inputs {
 implicit val inputs = new Writes[Inputs] {
  def writes(inputs: Inputs) = Json.obj(
   "address" -> inputs.address,
   "power-bill" -> inputs.power_bill,
   "kwh-usage" -> inputs.kwh_usage,
   "usage-offset" -> inputs.usage_offset,
   "array-type" -> inputs.array_type,
   "module-type" -> inputs.module_type,
   "loan-period" -> inputs.loan_period,
   "loan-interest" -> inputs.loan_interest
  )
 }
}

case class LocationInfo(
  var lat: BigDecimal,
  var lon: BigDecimal,
  var elev: Double,
  var tz: Double,
  var location: String,
  var city: String,
  var state: String
 )

object LocationInfo {
 implicit val locationInfoWrites = new Writes[LocationInfo] {
  def writes(info: LocationInfo) = Json.obj(
   "lat" -> info.lat,
   "lon" -> info.lon,
   "elev" -> info.elev,
   "tz" -> info.tz,
   "location" -> info.location,
   "city" -> info.city,
   "state" -> info.state
  )
 }
}

case class SavingsVsCosts(
  var cost: List[Double],
  var cost_total: Double,
  var cost_cumulative: List[Double],
  var savings:Option[List[Double]] = None,
  var savings_total:Option[Double] = None,
  var savings_cumulative:Option[List[Double]] = None,
  var year_one_savings_percent:Option[Double] = None,
  var monthly_cost:Option[Double] = None,
  var system_payment:Option[Double] = None,
  var srec_credits:Option[Double] = None,
  var extra_grid_power:Option[Double] = None,
  var net_monthly_savings:Option[Double] = None,
  var upfront_cost:Option[Double] = None,
  var effective_price:Option[Double] = None,
  var savings_percentage:Option[Double] = None
)

object SavingsVsCosts {
 implicit val savingsVsCostsWrites = new Writes[SavingsVsCosts] {
  def writes(savingsVsCosts: SavingsVsCosts) = {
   if (!savingsVsCosts.savings.isEmpty) {
    Json.obj(
     "savings" -> savingsVsCosts.savings.get,
     "savings_total" -> savingsVsCosts.savings_total.get,
     "savings_cumulative" -> savingsVsCosts.savings_cumulative.get,
     "year-1-savings-percentage" -> savingsVsCosts.year_one_savings_percent.get,
     "cost" -> savingsVsCosts.cost,
     "cost_total" -> savingsVsCosts.cost_total,
     "cost_cumulative" -> savingsVsCosts.cost_cumulative,
     "monthly-cost" -> savingsVsCosts.monthly_cost.get,
     "system-payment" -> savingsVsCosts.system_payment.get,
     "srec-credits" -> savingsVsCosts.srec_credits.get,
     "extra-grid-power" -> savingsVsCosts.extra_grid_power.get,
     "net-monthly-savings" -> savingsVsCosts.net_monthly_savings.get,
     "upfront-cost" -> savingsVsCosts.upfront_cost.get,
     "effective-price" -> savingsVsCosts.effective_price.get,
     "savings-percentage" -> savingsVsCosts.savings_percentage.get
    )
   } else {
    Json.obj(
     "cost" -> savingsVsCosts.cost,
     "cost_total" -> savingsVsCosts.cost_total,
     "cost_cumulative" -> savingsVsCosts.cost_cumulative
    )
   }
  }
 }
}

case class System (
  var system_size: Double,
  var current_usage: Double,
  var proposed_output: Double,
  var current_bill: Double
)

object System {
 implicit val system = new Writes[System] {
  def writes(output: System) = Json.obj(
   "system_size" -> output.system_size,
   "current-usage" -> output.current_usage,
   "proposed-output" -> output.proposed_output,
   "current-bill" -> output.current_bill
  )
 }
}

case class Outputs(
  var proposed_system: System,
  var cash: SavingsVsCosts,
  var ppa: SavingsVsCosts,
  var loan: SavingsVsCosts,
  var grid: SavingsVsCosts
)

object Outputs {
 implicit val outputsWrites = new Writes[Outputs] {
  def writes(outputs: Outputs) = Json.obj(
   "proposed-system" -> outputs.proposed_system,
   "cash" -> outputs.cash,
   "ppa" -> outputs.ppa,
   "loan" -> outputs.loan,
   "grid" -> outputs.grid
  )
 }
}

case class SolarCalculatorOutput (
  var inputs: Inputs,
  var versions: String,
  var location_info: LocationInfo,
  var outputs: Outputs,
  var confidence: Double
)

object SolarCalculatorOutput {
 implicit val solarCalculatorOutputWrites = new Writes[SolarCalculatorOutput] {
  def writes(output: SolarCalculatorOutput) = Json.obj(
   "inputs" -> output.inputs,
   "versions" -> output.versions,
   "location-info" -> output.location_info,
   "outputs" -> output.outputs,
   "confidence" -> output.confidence
  )
 }
}