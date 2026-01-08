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

import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase


class PaymentResponseSpec extends SpecBase {

  "PaymentResponse" - {

    "deserialize correctly from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "journeyId": "abc123",
          |  "nextUrl": "http://example.com"
          |}
            """.stripMargin
      )

      val expectedPaymentResponse = PaymentResponse(
        journeyId = "abc123",
        nextUrl = "http://example.com"
      )

      json.as[PaymentResponse] mustBe expectedPaymentResponse
    }

    "serialize correctly to JSON" in {
      val paymentResponse = PaymentResponse(
        journeyId = "abc123",
        nextUrl = "http://example.com"
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "journeyId": "abc123",
          |  "nextUrl": "http://example.com"
          |}
            """.stripMargin
      )

      Json.toJson(paymentResponse) mustBe expectedJson
    }

    "fail deserialization when required fields are missing" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "journeyId": "abc123"
          |}
            """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[PaymentResponse]
      }
    }

    "handle empty string values correctly" in {
      val json = Json.parse(
        """
          |{
          |  "journeyId": "",
          |  "nextUrl": ""
          |}
            """.stripMargin
      )

      val expectedPaymentResponse = PaymentResponse(
        journeyId = "",
        nextUrl = ""
      )

      json.as[PaymentResponse] mustBe expectedPaymentResponse
    }

    "fail deserialization when required fields are null" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "journeyId": null,
          |  "nextUrl": null
          |}
            """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[PaymentResponse]
      }
    }
  }
}
