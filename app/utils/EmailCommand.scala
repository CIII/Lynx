package utils

import com.tapquality.dao.MandrillDAO
import javax.inject.Inject
import scala.collection.JavaConversions._
import play.Logger


object EmailCommand {
  val EASIERSOLAR_TEMPLATE = "savings-email-030917-incl-ppa"
  val HOMESOLAR_TEMPLATE = "homesoalar-savings-email-080317-incl-ppa"
  val templates = Map(
    "easiersolar" -> EASIERSOLAR_TEMPLATE,
    "homesolar" -> HOMESOLAR_TEMPLATE
  )
}
/**
 * This class is acting as a command object
 */
class EmailCommand @Inject()(mandrillDAO : MandrillDAO) {
  
  def sendEmail(data : EmailCommandData, domain: String) {
    if( !data.firstName.equalsIgnoreCase("Mickey") || !data.lastName.equalsIgnoreCase("Mouse")) {
      val variables : Map[String, String] = createVariableMap(data)
      mandrillDAO.post(data.toEmail, variables, EmailCommand.templates.getOrElse(domain, EmailCommand.EASIERSOLAR_TEMPLATE), true)
    } else {
      Logger.info("Rejected email send; test detected")
    }
  }
  
  protected def createVariableMap(data : EmailCommandData) : Map[String, String] = {
    return Map(
        "first_name" -> data.firstName,
        "city" -> data.city.getOrElse("your city"),
        "electric_bill" -> data.electricBill,
        "current_annual_usage" -> data.currentAnnualUsage,
        "proposed_system_size" -> data.proposedSystemSize,
        "loan_total_savings" -> data.loanTotalSavings,
        "loan_monthly_cost" -> data.loanMonthlyCost,
        "ppa_monthly_cost" -> data.ppaMonthlyCost,
        "cash_monthly_cost" -> data.cashMonthlyCost,
        "loan_system_payment" -> data.loanSystemPayment,
        "ppa_system_payment" -> data.ppaSystemPayment,
        "cash_system_payment" -> data.cashSystemPayment,
        "loan_srec_income" -> data.loanSrecIncome,
        "ppa_srec_income" -> data.ppaSrecIncome,
        "cash_srec_income" -> data.cashSrecIncome,
        "loan_new_power_bill" -> data.loanNewPowerBill,
        "ppa_new_power_bill" -> data.ppaNewPowerBill,
        "cash_new_power_bill" -> data.cashNewPowerBill,
        "loan_monthly_savings" -> data.loanMonthlySavings,
        "ppa_monthly_savings" -> data.ppaMonthlySavings,
        "cash_monthly_savings" -> data.cashMonthlySavings,
        "loan_y1_savings" -> data.loanY1Savings,
        "ppa_y1_savings" -> data.ppaY1Savings,
        "cash_y1_savings" -> data.cashY1Savings,
        "loan_lt_savings" -> data.loanLtSavings,
        "ppa_lt_savings" -> data.ppaLtSavings,
        "cash_lt_savings" -> data.cashLtSavings,
        "loan_upfront_cost" -> data.loanUpfrontCost,
        "ppa_upfront_cost" -> data.ppaUpfrontCost,
        "cash_upfront_cost" -> data.cashUpfrontCost,
        "loan_new_ppkwh" -> data.loanNewPpkwh,
        "ppa_new_ppkwh" -> data.ppaNewPpkwh,
        "cash_new_ppkwh" -> data.cashNewPpkwh,
        "loan_savings_pct" -> data.loanSavingsPct,
        "ppa_savings_pct" -> data.ppaSavingsPct,
        "cash_savings_pct" -> data.cashSavingsPct
    )
  }
}

case class EmailCommandData (
    toEmail : String,
    firstName : String,
    lastName : String,
    city: Option[String],
    electricBill : String,
    currentAnnualUsage : String,
    proposedSystemSize : String,
    loanTotalSavings : String,
    loanMonthlyCost : String,
    ppaMonthlyCost : String,
    cashMonthlyCost : String,
    loanSystemPayment : String,
    ppaSystemPayment : String,
    cashSystemPayment : String,
    loanSrecIncome : String,
    ppaSrecIncome : String,
    cashSrecIncome : String,
    loanNewPowerBill : String,
    ppaNewPowerBill : String,
    cashNewPowerBill : String,
    loanMonthlySavings : String,
    ppaMonthlySavings : String,
    cashMonthlySavings : String,
    loanY1Savings : String,
    ppaY1Savings : String,
    cashY1Savings : String,
    loanLtSavings : String,
    ppaLtSavings : String,
    cashLtSavings : String,
    loanUpfrontCost : String,
    ppaUpfrontCost : String,
    cashUpfrontCost : String,
    loanNewPpkwh : String,
    ppaNewPpkwh : String,
    cashNewPpkwh : String,
    loanSavingsPct : String,
    ppaSavingsPct : String,
    cashSavingsPct : String)