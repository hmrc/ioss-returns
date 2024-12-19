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

package uk.gov.hmrc.iossreturns.models.payments

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.StandardPeriod

import java.time.{LocalDate, Month}

class PaymentSpec extends SpecBase with ScalaCheckPropertyChecks {

  "Payment" - {

    "must serialize and deserialize correctly" in {
      val payment = Payment(
        period = StandardPeriod(2023, Month.JANUARY),
        amountOwed = BigDecimal(100.00),
        dateDue = LocalDate.of(2023, 2, 28),
        paymentStatus = PaymentStatus.Unpaid
      )

      val expectedJson = Json.parse(
        s"""
           |{
           |  "period": {
           |    "year": 2023,
           |    "month": "M1"
           |  },
           |  "amountOwed": 100.00,
           |  "dateDue": "2023-02-28",
           |  "paymentStatus": "UNPAID"
           |}
            """.stripMargin
      )

      val json = Json.toJson(payment)

      json mustBe expectedJson

      val deserialized = json.as[Payment]

      deserialized mustBe payment
    }

    "must handle edge cases" - {
      "with zero amount owed" in {
        val payment = Payment(
          period = StandardPeriod(2023, Month.JANUARY),
          amountOwed = BigDecimal(0.00),
          dateDue = LocalDate.of(2023, 2, 28),
          paymentStatus = PaymentStatus.Unknown
        )

        val json = Json.toJson(payment)
        val deserialized = json.as[Payment]

        deserialized mustBe payment
      }

      "with negative amount owed if valid" in {
        val payment = Payment(
          period = StandardPeriod(2023, Month.JANUARY),
          amountOwed = BigDecimal(-50.00),
          dateDue = LocalDate.of(2023, 2, 28),
          paymentStatus = PaymentStatus.Partial
        )

        val json = Json.toJson(payment)
        val deserialized = json.as[Payment]

        deserialized mustBe payment
      }

      "with different periods" in {
        val payment = Payment(
          period = StandardPeriod(2024, Month.DECEMBER),
          amountOwed = BigDecimal(150.00),
          dateDue = LocalDate.of(2025, 1, 31),
          paymentStatus = PaymentStatus.Excluded
        )

        val json = Json.toJson(payment)
        val deserialized = json.as[Payment]

        deserialized mustBe payment
      }
    }

    "must fail deserialization for invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "amountOwed": 100.00,
          |  "dateDue": "2023-02-28",
          |  "paymentStatus": "UNPAID"
          |}
            """.stripMargin // Missing the "period" field
      )

      intercept[JsResultException] {
        invalidJson.as[Payment]
      }
    }

    "must fail deserialization for invalid PaymentStatus" in {
      val invalidJson = Json.parse(
        s"""
           |{
           |  "period": {
           |    "year": 2023,
           |    "month": "M1"
           |  },
           |  "amountOwed": 100.00,
           |  "dateDue": "2023-02-28",
           |  "paymentStatus": "INVALID_STATUS"
           |}
            """.stripMargin
      )

      intercept[MatchError] {
        invalidJson.as[Payment]
      }
    }
  }
}
