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

package uk.gov.hmrc.iossreturns.models.financialdata

import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.{Period, StandardPeriod}
import uk.gov.hmrc.iossreturns.models.payments.Charge

import java.time.{LocalDate, Month, ZoneOffset, ZonedDateTime}

class FinancialDataSpec extends SpecBase {
  protected val zonedNow: ZonedDateTime = ZonedDateTime.of(2023, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  protected val zonedDateTimeNow: ZonedDateTime = ZonedDateTime.now(stubClockAtArbitraryDate).plusSeconds(1)
  protected val dateFrom: LocalDate = zonedNow.toLocalDate.minusMonths(1)
  protected val dateTo: LocalDate = zonedNow.toLocalDate
  protected val item: Item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))

  val originalAmount1: BigDecimal = BigDecimal(200)
  val clearedAmount1: BigDecimal = BigDecimal(50)
  val outstandingAmount1: BigDecimal = BigDecimal(150)

  val originalAmount2: BigDecimal = BigDecimal(1000)
  val clearedAmount2: BigDecimal = BigDecimal(150)
  val outstandingAmount2: BigDecimal = BigDecimal(350)

  protected val financialTransaction: FinancialTransaction = FinancialTransaction(Some("G Ret AT EU-OMS"), None, Some(dateFrom), Some(dateTo), None, None, None, Some(Seq(item)))
  val ft1: FinancialTransaction = generateFinancialTransaction(None, Some(originalAmount1), Some(outstandingAmount1), Some(clearedAmount1))
  val ft2: FinancialTransaction = generateFinancialTransaction(None, Some(originalAmount2), Some(outstandingAmount2), Some(clearedAmount2))
  val financialData: FinancialData = FinancialData(None, None, None, zonedDateTimeNow, Some(List(ft1, ft2)))
  private val financialTransaction2 = FinancialTransaction(Some("G Ret AT EU-OMS"), None, Some(dateFrom), Some(dateTo), Some(1000), Some(500), Some(500), Some(Seq(item)))
  private val financialDataTransaction = FinancialData(Some("IOSS"), Some("123456789"), Some("ECOM"), zonedDateTimeNow, Some(Seq(financialTransaction2)))
  private def generateFinancialTransaction(
                                            period: Option[Period],
                                            originalAmount: Option[BigDecimal],
                                            outstandingAmount: Option[BigDecimal],
                                            clearedAmount: Option[BigDecimal]
                                          ): FinancialTransaction = {
    financialTransaction
      .copy(
        taxPeriodFrom = period.map(_.firstDay).fold(financialTransaction.taxPeriodFrom)(Some(_)),
        originalAmount = originalAmount,
        outstandingAmount = outstandingAmount,
        clearedAmount = clearedAmount
      )

  }


  private val financialDataJson =
    s"""{
       | "idType": "IOSS",
       | "idNumber": "123456789",
       | "regimeType": "ECOM",
       | "processingDate": "${zonedDateTimeNow.toString}",
       | "financialTransactions": [
       |   {
       |     "chargeType": "G Ret AT EU-OMS",
       |     "taxPeriodFrom": "$dateFrom",
       |     "taxPeriodTo": "$dateTo",
       |     "originalAmount": 1000,
       |     "outstandingAmount": 500,
       |     "clearedAmount": 500,
       |     "items": [
       |       {
       |         "amount": 500,
       |         "clearingReason": "",
       |         "paymentReference": "",
       |         "paymentAmount": 500,
       |         "paymentMethod": ""
       |       }
       |     ]
       |   }
       | ]
       |}""".stripMargin

  "FinancialData" - {
    "must deserialise correctly" in {
      Json.parse(financialDataJson).as[FinancialData] mustBe financialDataTransaction
    }

    "must serialise correctly`" in {
      Json.toJson(financialDataTransaction) mustBe Json.parse(financialDataJson)
    }
  }

  "FinancialData" - {

    "must not generate charge when all transactions are within given period" in {
      val period = StandardPeriod(dateFrom.getYear, dateFrom.getMonth)

      financialData.getChargeForPeriod(period) mustBe Some(Charge(
        period,
        originalAmount1 + originalAmount2,
        outstandingAmount1 + outstandingAmount2,
        clearedAmount1 + clearedAmount2
      ))
    }

    "must not generate charge when all transactions are within a different period than the given period" in {
      val differentPeriodThanTransactions = StandardPeriod(2020, Month.JANUARY)

      financialData.getChargeForPeriod(differentPeriodThanTransactions) mustBe None
    }

    "must not generate charge from the transactions of the given period and neglect the transactions not from the given period" in {
      val period = StandardPeriod(2021, Month.MARCH)
      val otherPeriod = StandardPeriod(2020, Month.MARCH)

      val ft1 = generateFinancialTransaction(Some(otherPeriod), Some(originalAmount1), Some(outstandingAmount1), Some(clearedAmount1))
      val ft2 = generateFinancialTransaction(Some(period), Some(originalAmount2), Some(outstandingAmount2), Some(clearedAmount2))
      val ft3 = generateFinancialTransaction(Some(period), Some(originalAmount1), Some(outstandingAmount1), Some(clearedAmount1))
      val ft4 = generateFinancialTransaction(Some(otherPeriod), Some(originalAmount2), Some(outstandingAmount2), Some(clearedAmount2))

      financialData.copy(financialTransactions = Some(List(ft1, ft2, ft3, ft4))).getChargeForPeriod(period) mustBe Some(Charge(
        period,
        originalAmount1 + originalAmount2,
        outstandingAmount1 + outstandingAmount2,
        clearedAmount1 + clearedAmount2
      ))
    }
  }
}
