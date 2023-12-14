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

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class TraderIdSpec extends SpecBase {

  "TraderId" - {

    "must deserialise/serialise to and from VatNumberTraderId" in {

      val vatNumberTraderId = arbitrary[VatNumberTraderId].sample.value

      val expectedJson = Json.obj(
        "vatNumber" -> s"${vatNumberTraderId.vatNumber}"
      )

      Json.toJson(vatNumberTraderId) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "must deserialise/serialise to and from TaxRefTraderID" in {

      val taxRefTraderID = arbitrary[TaxRefTraderID].sample.value

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> s"${taxRefTraderID.taxReferenceNumber}"
      )

      Json.toJson(taxRefTraderID) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
    }
  }
}

