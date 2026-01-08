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

package uk.gov.hmrc.iossreturns.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class CountrySpec extends AnyFreeSpec with Matchers {

  "Country" - {

    "serialize and deserialize correctly" in {
      val country = Country("DE", "Germany")

      val json = Json.toJson(country)
      json mustBe Json.obj(
        "code" -> "DE",
        "name" -> "Germany"
      )

      val deserialized = json.as[Country]
      deserialized mustEqual country
    }

    "serialize and deserialize with another Country" in {
      val country = Country("FR", "France")

      val json = Json.toJson(country)
      json mustBe Json.obj(
        "code" -> "FR",
        "name" -> "France"
      )

      val deserialized = json.as[Country]
      deserialized mustEqual country
    }

    "handle invalid JSON" in {
      val invalidJson = Json.obj(
        "code" -> "IT",
      )

      val result = invalidJson.validate[Country]
      result.isError mustBe true
    }

    "handle missing 'code' field" in {
      val invalidJson = Json.obj(
        "name" -> "Germany"
      )

      val result = invalidJson.validate[Country]
      result.isError mustBe true
    }

    "serialize a Country with a long name" in {
      val country = Country("TH", "Thailand")

      val json = Json.toJson(country)
      json mustBe Json.obj(
        "code" -> "TH",
        "name" -> "Thailand"
      )

      val deserialized = json.as[Country]
      deserialized mustEqual country
    }

    "deserialize a Country with unknown code" in {
      val unknownJson = Json.obj(
        "code" -> "ZZ",
        "name" -> "UnknownLand"
      )

      val deserialized = unknownJson.as[Country]
      deserialized mustEqual Country("ZZ", "UnknownLand")
    }
  }
}
