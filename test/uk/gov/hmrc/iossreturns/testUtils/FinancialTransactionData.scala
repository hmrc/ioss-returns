package uk.gov.hmrc.iossreturns.testUtils

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}

import java.time.ZonedDateTime


object FinancialTransactionData extends SpecBase {

  private def financialTransactionData(
                                        period: Period,
                                        originalAmount: BigDecimal,
                                        outstandingAmount: BigDecimal,
                                        clearedAmount: BigDecimal
                                      ): FinancialTransaction = {
    FinancialTransaction(
      chargeType = Some(s"G Ret AT EU-OMS"),
      mainType = Some(arbitrary[String].sample.value),
      taxPeriodFrom = Some(period.firstDay),
      taxPeriodTo = Some(period.lastDay),
      originalAmount = Some(originalAmount),
      outstandingAmount = Some(outstandingAmount),
      clearedAmount = Some(clearedAmount),
      items = Some(Seq(itemData))
    )
  }

  def itemData: Item = Item(
    amount = Some(arbitrary[BigDecimal].sample.value),
    clearingReason = Some(arbitrary[String].sample.value),
    paymentReference = Some(arbitrary[String].sample.value),
    paymentAmount = Some(arbitrary[BigDecimal].sample.value),
    paymentMethod = Some(arbitrary[String].sample.value)
  )

  def getFinancialData(
                        period: Period,
                        originalAmount: BigDecimal,
                        outstandingAmount: BigDecimal,
                        clearedAmount: BigDecimal,
                        numberOfTransactions: Int
                      ): FinancialData =
    FinancialData(
      idType = Some("IOSS"),
      idNumber = Some(arbitrary[String].sample.value),
      regimeType = Some("ECOM"),
      processingDate = arbitrary[ZonedDateTime].sample.value,
      financialTransactions = Some(Gen.listOfN(numberOfTransactions, financialTransactionData(period, originalAmount, outstandingAmount, clearedAmount)).sample.value)
    )
}
