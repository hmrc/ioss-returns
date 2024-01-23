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

import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Format, Reads, Writes}

import java.time.{LocalDate, LocalDateTime}

case class EtmpVatReturn(
                          returnReference: String,
                          returnVersion: LocalDateTime,
                          periodKey: String,
                          returnPeriodFrom: LocalDate,
                          returnPeriodTo: LocalDate,
                          goodsSupplied: Seq[EtmpVatReturnGoodsSupplied],
                          totalVATGoodsSuppliedGBP: BigDecimal,
                          totalVATAmountPayable: BigDecimal,
                          totalVATAmountPayableAllSpplied: BigDecimal,
                          correctionPreviousVATReturn: Seq[EtmpVatReturnCorrection],
                          totalVATAmountFromCorrectionGBP: BigDecimal,
                          balanceOfVATDueForMS: Seq[EtmpVatReturnBalanceOfVatDue],
                          totalVATAmountDueForAllMSGBP: BigDecimal,
                          paymentReference: String
                        )

object EtmpVatReturn {

  implicit val reads: Reads[EtmpVatReturn] = {
    (
      (__ \ "returnReference").read[String] and
        (__ \ "returnVersion").read[LocalDateTime] and
        (__ \ "periodKey").read[String] and
        (__ \ "returnPeriodFrom").read[LocalDate] and
        (__ \ "returnPeriodTo").read[LocalDate] and
        (__ \ "goodsSupplied").readWithDefault[Seq[EtmpVatReturnGoodsSupplied]](Seq.empty) and
        (__ \ "totalVATGoodsSuppliedGBP").read[BigDecimal] and
        (__ \ "totalVATAmountPayable").read[BigDecimal] and
        (__ \ "totalVATAmountPayableAllSpplied").read[BigDecimal] and
        (__ \ "correctionPreviousVATReturn").readWithDefault[Seq[EtmpVatReturnCorrection]](Seq.empty) and
        (__ \ "totalVATAmountFromCorrectionGBP").read[BigDecimal] and
        (__ \ "balanceOfVATDueForMS").readWithDefault[Seq[EtmpVatReturnBalanceOfVatDue]](Seq.empty) and
        (__ \ "totalVATAmountDueForAllMSGBP").read[BigDecimal] and
        (__ \ "paymentReference").read[String]
      )(EtmpVatReturn.apply _)
  }

  implicit val writes: Writes[EtmpVatReturn] = {
    (
      (__ \ "returnReference").write[String] and
        (__ \ "returnVersion").write[LocalDateTime] and
        (__ \ "periodKey").write[String] and
        (__ \ "returnPeriodFrom").write[LocalDate] and
        (__ \ "returnPeriodTo").write[LocalDate] and
        (__ \ "goodsSupplied").write[Seq[EtmpVatReturnGoodsSupplied]] and
        (__ \ "totalVATGoodsSuppliedGBP").write[BigDecimal] and
        (__ \ "totalVATAmountPayable").write[BigDecimal] and
        (__ \ "totalVATAmountPayableAllSpplied").write[BigDecimal] and
        (__ \ "correctionPreviousVATReturn").write[Seq[EtmpVatReturnCorrection]] and
        (__ \ "totalVATAmountFromCorrectionGBP").write[BigDecimal] and
        (__ \ "balanceOfVATDueForMS").write[Seq[EtmpVatReturnBalanceOfVatDue]] and
        (__ \ "totalVATAmountDueForAllMSGBP").write[BigDecimal] and
        (__ \ "paymentReference").write[String]
      )(unlift(EtmpVatReturn.unapply))
  }

  implicit val format: Format[EtmpVatReturn] = Format(reads, writes)

  implicit class VatAmountCalculations(vatReturn: EtmpVatReturn) {
    def getTotalVatOnSalesAfterCorrection(): BigDecimal = {

      val runningTotal = getTotalVatAmountBeforeCorrection() + vatReturn.totalVATAmountFromCorrectionGBP

      if (runningTotal < 0) {
        BigDecimal(0)
      } else {
        runningTotal
      }
    }

    private def getTotalVatAmountBeforeCorrection(): BigDecimal = {
      vatReturn.goodsSupplied.map(_.vatAmountGBP).sum
    }
  }
}
