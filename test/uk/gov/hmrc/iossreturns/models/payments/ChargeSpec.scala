/*
 * Copyright 2026 HM Revenue & Customs
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

import java.time.Month

class ChargeSpec extends SpecBase with ScalaCheckPropertyChecks {

  "Charge" - {
    "must" - {
      val chargeOutStandingAmountEqualToOriginalAmount = Charge(StandardPeriod(2020, Month.MAY), BigDecimal(10), BigDecimal(10), BigDecimal(10))

      val chargeOutStandingAmountNotEqualToOriginalAmount = Charge(StandardPeriod(2020, Month.MAY), BigDecimal(10), BigDecimal(7), BigDecimal(3))

      val scenarios = Table[Option[Charge], PaymentStatus](
        ("Charge", "Payment Status"),
        (
          None,
          PaymentStatus.Unknown
        ),
        (
          Some(chargeOutStandingAmountEqualToOriginalAmount),
          PaymentStatus.Unpaid
        ),
        (
          Some(chargeOutStandingAmountNotEqualToOriginalAmount),
          PaymentStatus.Partial
        )
      )

      forAll(scenarios) { (c, ps) =>
        s"correctly detect Payment Status $ps" in {
          c.getPaymentStatus() mustBe ps
        }
      }
    }
  }

  "Json.format[Charge]" - {

    "must serialize and deserialize correctly" in {
      val charge = Charge(
        period = StandardPeriod(2023, Month.JANUARY),
        originalAmount = BigDecimal(100.00),
        outstandingAmount = BigDecimal(50.00),
        clearedAmount = BigDecimal(50.00)
      )

      val expectedJson = Json.parse(
        s"""
           |{
           |  "period": {
           |    "year": 2023,
           |    "month": "M1"
           |  },
           |  "originalAmount": 100.00,
           |  "outstandingAmount": 50.00,
           |  "clearedAmount": 50.00
           |}
        """.stripMargin
      )

      val json = Json.toJson(charge)

      json mustBe expectedJson

      val deserialized = json.as[Charge]

      deserialized mustBe charge
    }

    "must fail deserialization for invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "originalAmount": 100.00,
          |  "outstandingAmount": 50.00,
          |  "clearedAmount": 50.00
          |}
        """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[Charge]
      }
    }
  }

  "Charge JSON format edge cases" - {

    "must handle zero amounts" in {
      val charge = Charge(
        period = StandardPeriod(2023, Month.JANUARY),
        originalAmount = BigDecimal(0.00),
        outstandingAmount = BigDecimal(0.00),
        clearedAmount = BigDecimal(0.00)
      )

      val json = Json.toJson(charge)
      val deserialized = json.as[Charge]

      deserialized mustBe charge
    }

    "must handle negative amounts if valid" in {
      val charge = Charge(
        period = StandardPeriod(2023, Month.JANUARY),
        originalAmount = BigDecimal(-100.00),
        outstandingAmount = BigDecimal(-50.00),
        clearedAmount = BigDecimal(-50.00)
      )

      val json = Json.toJson(charge)
      val deserialized = json.as[Charge]

      deserialized mustBe charge
    }

    "must handle different periods" in {
      val charge = Charge(
        period = StandardPeriod(2024, Month.DECEMBER),
        originalAmount = BigDecimal(100.00),
        outstandingAmount = BigDecimal(75.00),
        clearedAmount = BigDecimal(25.00)
      )

      val json = Json.toJson(charge)
      val deserialized = json.as[Charge]

      deserialized mustBe charge
    }

    "must fail deserialization for invalid amounts" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M1"
          |  },
          |  "originalAmount": "invalid",
          |  "outstandingAmount": 50.00,
          |  "clearedAmount": 50.00
          |}
        """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[Charge]
      }
    }

  }
}
