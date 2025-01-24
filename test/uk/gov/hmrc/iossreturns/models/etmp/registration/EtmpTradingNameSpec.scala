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

package uk.gov.hmrc.iossreturns.models.etmp.registration

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpTradingNameSpec extends SpecBase {

  "EtmpTradingName" - {

    "must serialise/deserialise to and from EtmpTradingName" in {

      val etmpTradingName = arbitrary[EtmpTradingName].sample.value

      val expectedJson = Json.obj(
        "tradingName" -> s"${etmpTradingName.tradingName}"
      )

      Json.toJson(etmpTradingName) mustBe expectedJson
      expectedJson.validate[EtmpTradingName] mustBe JsSuccess(etmpTradingName)
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "tradingName" -> "Test Trading Name"
      )

      val expectedTradingName = EtmpTradingName("Test Trading Name")
      json.validate[EtmpTradingName] mustBe JsSuccess(expectedTradingName)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()


      json.validate[EtmpTradingName] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "tradingName" -> 12345
      )

      json.validate[EtmpTradingName] mustBe a[JsError]
    }
  }
}
