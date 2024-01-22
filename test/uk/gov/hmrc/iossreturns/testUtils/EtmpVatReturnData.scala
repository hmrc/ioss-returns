package uk.gov.hmrc.iossreturns.testUtils

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.etmp._

import java.time.{LocalDate, LocalDateTime}

object EtmpVatReturnData extends SpecBase {

  private val amountOfGoodsSupplied: Int = Gen.oneOf(List(1, 2, 3)).sample.value
  private val amountOfCorrections: Int = Gen.oneOf(List(1, 2, 3)).sample.value
  private val amountOfBalanceOfVATDueForMS: Int = Gen.oneOf(List(1, 2, 3)).sample.value

  val etmpVatReturnGoodsSupplied: EtmpVatReturnGoodsSupplied = EtmpVatReturnGoodsSupplied(
    msOfConsumption = arbitrary[String].sample.value,
    vatRateType = Gen.oneOf(EtmpVatRateType.values).sample.value,
    taxableAmountGBP = arbitrary[BigDecimal].sample.value,
    vatAmountGBP = arbitrary[BigDecimal].sample.value
  )

  val etmpVatReturnCorrection: EtmpVatReturnCorrection = EtmpVatReturnCorrection(
    periodKey = arbitrary[String].sample.value,
    periodFrom = arbitrary[String].sample.value,
    periodTo = arbitrary[String].sample.value,
    msOfConsumption = arbitrary[String].sample.value,
    totalVATAmountCorrectionGBP = arbitrary[BigDecimal].sample.value,
    totalVATAmountCorrectionEUR = arbitrary[BigDecimal].sample.value
  )

  val etmpVatReturnBalanceOfVatDue: EtmpVatReturnBalanceOfVatDue = EtmpVatReturnBalanceOfVatDue(
    msOfConsumption = arbitrary[String].sample.value,
    totalVATDueGBP = arbitrary[BigDecimal].sample.value,
    totalVATEUR = arbitrary[BigDecimal].sample.value
  )

  val etmpVatReturn: EtmpVatReturn = EtmpVatReturn(
    returnReference = arbitrary[String].sample.value,
    returnVersion = arbitrary[LocalDateTime].sample.value,
    periodKey = arbitrary[String].sample.value,
    returnPeriodFrom = arbitrary[LocalDate].sample.value,
    returnPeriodTo = arbitrary[LocalDate].sample.value,
    goodsSupplied = Gen.listOfN(amountOfGoodsSupplied, etmpVatReturnGoodsSupplied).sample.value,
    totalVATGoodsSuppliedGBP = arbitrary[BigDecimal].sample.value,
    totalVATAmountPayable = arbitrary[BigDecimal].sample.value,
    totalVATAmountPayableAllSpplied = arbitrary[BigDecimal].sample.value,
    correctionPreviousVATReturn = Gen.listOfN(amountOfCorrections, etmpVatReturnCorrection).sample.value,
    totalVATAmountFromCorrectionGBP = arbitrary[BigDecimal].sample.value,
    balanceOfVATDueForMS = Gen.listOfN(amountOfBalanceOfVATDueForMS, etmpVatReturnBalanceOfVatDue).sample.value,
    totalVATAmountDueForAllMSGBP = arbitrary[BigDecimal].sample.value,
    paymentReference = arbitrary[String].sample.value
  )
}
