/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossreturns.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.testUtils.EtmpVatReturnData.etmpVatReturn

import java.time.{LocalDate, LocalDateTime}

class EtmpVatReturnSpec extends SpecBase {
  val goodSupplied1: EtmpVatReturnGoodsSupplied = EtmpVatReturnGoodsSupplied("IE", EtmpVatRateType.ReducedVatRate, BigDecimal(100), BigDecimal(15))
  val goodSupplied2: EtmpVatReturnGoodsSupplied = EtmpVatReturnGoodsSupplied("IT", EtmpVatRateType.StandardVatRate, BigDecimal(200), BigDecimal(20))

  val etmpVatReturnWithoutCorrection: EtmpVatReturn = EtmpVatReturn("", LocalDateTime.now(), "", LocalDate.now(), LocalDate.now(), List(goodSupplied1, goodSupplied2), BigDecimal(0), BigDecimal(0),
    BigDecimal(0), Nil, BigDecimal(0), Nil, BigDecimal(0), "")

  val etmpVatReturnWithCorrection: EtmpVatReturn = etmpVatReturnWithoutCorrection.copy(totalVATAmountFromCorrectionGBP = BigDecimal(2.5))

  private val genEtmpVatReturn: EtmpVatReturn = etmpVatReturn

  "EtmpVatReturn" - {

    "must serialise/deserialise to and from EtmpVatReturn" in {

      val json = Json.obj(
        "returnReference" -> genEtmpVatReturn.returnReference,
        "returnVersion" -> genEtmpVatReturn.returnVersion,
        "periodKey" -> genEtmpVatReturn.periodKey,
        "returnPeriodFrom" -> genEtmpVatReturn.returnPeriodFrom,
        "returnPeriodTo" -> genEtmpVatReturn.returnPeriodTo,
        "goodsSupplied" -> genEtmpVatReturn.goodsSupplied,
        "totalVATGoodsSuppliedGBP" -> genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        "totalVATAmountPayable" -> genEtmpVatReturn.totalVATAmountPayable,
        "totalVATAmountPayableAllSpplied" -> genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        "correctionPreviousVATReturn" -> genEtmpVatReturn.correctionPreviousVATReturn,
        "totalVATAmountFromCorrectionGBP" -> genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        "balanceOfVATDueForMS" -> genEtmpVatReturn.balanceOfVATDueForMS,
        "totalVATAmountDueForAllMSGBP" -> genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        "paymentReference" -> genEtmpVatReturn.paymentReference
      )

      val expectedResult = EtmpVatReturn(
        returnReference = genEtmpVatReturn.returnReference,
        returnVersion = genEtmpVatReturn.returnVersion,
        periodKey = genEtmpVatReturn.periodKey,
        returnPeriodFrom = genEtmpVatReturn.returnPeriodFrom,
        returnPeriodTo = genEtmpVatReturn.returnPeriodTo,
        goodsSupplied = genEtmpVatReturn.goodsSupplied,
        totalVATGoodsSuppliedGBP = genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        totalVATAmountPayable = genEtmpVatReturn.totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = genEtmpVatReturn.correctionPreviousVATReturn,
        totalVATAmountFromCorrectionGBP = genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = genEtmpVatReturn.balanceOfVATDueForMS,
        totalVATAmountDueForAllMSGBP = genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        paymentReference = genEtmpVatReturn.paymentReference
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturn] mustBe JsSuccess(expectedResult)
    }

    "must deserialise to EtmpVatReturn with missing sequences as empty" in {

      val json = Json.obj(
        "returnReference" -> genEtmpVatReturn.returnReference,
        "returnVersion" -> genEtmpVatReturn.returnVersion,
        "periodKey" -> genEtmpVatReturn.periodKey,
        "returnPeriodFrom" -> genEtmpVatReturn.returnPeriodFrom,
        "returnPeriodTo" -> genEtmpVatReturn.returnPeriodTo,
        "totalVATGoodsSuppliedGBP" -> genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        "totalVATAmountPayable" -> genEtmpVatReturn.totalVATAmountPayable,
        "totalVATAmountPayableAllSpplied" -> genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        "totalVATAmountFromCorrectionGBP" -> genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        "totalVATAmountDueForAllMSGBP" -> genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        "paymentReference" -> genEtmpVatReturn.paymentReference
      )

      val expectedResult = EtmpVatReturn(
        returnReference = genEtmpVatReturn.returnReference,
        returnVersion = genEtmpVatReturn.returnVersion,
        periodKey = genEtmpVatReturn.periodKey,
        returnPeriodFrom = genEtmpVatReturn.returnPeriodFrom,
        returnPeriodTo = genEtmpVatReturn.returnPeriodTo,
        goodsSupplied = Seq.empty,
        totalVATGoodsSuppliedGBP = genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        totalVATAmountPayable = genEtmpVatReturn.totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = Seq.empty,
        totalVATAmountFromCorrectionGBP = genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = Seq.empty,
        totalVATAmountDueForAllMSGBP = genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        paymentReference = genEtmpVatReturn.paymentReference
      )

      json.validate[EtmpVatReturn] mustBe JsSuccess(expectedResult)
    }

    "should getTotalVatOnSalesAfterCorrection correctly when there is no correction" in {
      etmpVatReturnWithoutCorrection.getTotalVatOnSalesAfterCorrection() mustBe BigDecimal(35)
    }
    "should getTotalVatOnSalesAfterCorrection correctly when there is correction" in {
      etmpVatReturnWithCorrection.getTotalVatOnSalesAfterCorrection() mustBe BigDecimal(37.5)
    }
  }
}
