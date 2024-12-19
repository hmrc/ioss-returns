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

import play.api.libs.json._
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.iossreturns.base.SpecBase
import java.time.LocalDate


class FinancialDataQueryParametersSpec extends SpecBase with Matchers {

    "FinancialDataQueryParameters" - {

      "must serialize and deserialize correctly" in {
        val parameters = FinancialDataQueryParameters(
          fromDate = Some(LocalDate.of(2023, 1, 1)),
          toDate = Some(LocalDate.of(2023, 12, 31)),
          onlyOpenItems = Some(true),
          includeLocks = Some(false),
          calculateAccruedInterest = Some(true),
          customerPaymentInformation = Some(false)
        )

        val json = Json.toJson(parameters)

        val expectedJson = Json.obj(
          "fromDate" -> "2023-01-01",
          "toDate" -> "2023-12-31",
          "onlyOpenItems" -> true,
          "includeLocks" -> false,
          "calculateAccruedInterest" -> true,
          "customerPaymentInformation" -> false
        )

        json mustEqual expectedJson

        val deserialized = json.as[FinancialDataQueryParameters]
        deserialized mustEqual parameters
      }

      "must handle empty optional fields during serialization" in {
        val parameters = FinancialDataQueryParameters()

        val json = Json.toJson(parameters)

        val expectedJson = Json.obj()

        json mustEqual expectedJson

        val deserialized = json.as[FinancialDataQueryParameters]
        deserialized mustEqual parameters
      }

      "toSeqQueryParams must generate the correct query parameters" in {
        val parameters = FinancialDataQueryParameters(
          fromDate = Some(LocalDate.of(2023, 1, 1)),
          toDate = Some(LocalDate.of(2023, 12, 31)),
          onlyOpenItems = Some(true),
          includeLocks = None,
          calculateAccruedInterest = Some(false),
          customerPaymentInformation = None
        )

        val queryParams = parameters.toSeqQueryParams

        val expectedQueryParams = Seq(
          "dateFrom" -> "2023-01-01",
          "dateTo" -> "2023-12-31",
          "onlyOpenItems" -> "true",
          "calculateAccruedInterest" -> "false"
        )

        queryParams mustEqual expectedQueryParams
      }

      "toSeqQueryParams must handle empty optional fields gracefully" in {
        val parameters = FinancialDataQueryParameters()

        val queryParams = parameters.toSeqQueryParams

        queryParams mustBe empty
      }

      "must ignore invalid field formats during deserialization" in {
        val invalidJson = Json.obj(
          "fromDate" -> "invalid-date",
          "toDate" -> "2023-12-31",
          "onlyOpenItems" -> true
        )

        intercept[JsResultException] {
          invalidJson.as[FinancialDataQueryParameters]
        }
      }
    }
}
