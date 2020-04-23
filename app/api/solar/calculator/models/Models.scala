package api.solar.calculator.models

/**
  * Created by slin on 2/15/17.
  */

case class CostByState(
  var state: String,
  var sum_of_customers: Long,
  var sum_of_sales: Long,
  var sum_of_revenues: Long,
  var average_kwh_per_user: BigDecimal,
  var cost: Float
)

case class CostByZip(
  val zip_code: Int,
  var cost: Float,
  val state: String
)

case class SolarYield(
  val zip_code: Int,
  var ac_monthly_1: Float,
  var ac_monthly_2: Float,
  var ac_monthly_3: Float,
  var ac_monthly_4: Float,
  var ac_monthly_5: Float,
  var ac_monthly_6: Float,
  var ac_monthly_7: Float,
  var ac_monthly_8: Float,
  var ac_monthly_9: Float,
  var ac_monthly_10: Float,
  var ac_monthly_11: Float,
  var ac_monthly_12: Float,
  var ac_annual: Float,
  var solrad_annual: Float,
  var capacity_factor: Float
)

case class SREC(
  val state: String,
  var srec: Int
)

case class SystemCost(
  val state: String,
  var low: Float,
  var high: Float
)

case class Utility(
  val id:Long,
  var name: String,
  var state: String,
  var sum_of_customers: Long,
  var sum_of_sales: Long,
  var sum_of_revenues: Long,
  var average_kwh_per_user: BigDecimal,
  var cost: Float
)
