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

import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpWebsiteSpec extends SpecBase {

  "EtmpWebsite" - {

    "must serialize to JSON correctly" in {
      val website = EtmpWebsite("http://example.com")

      val expectedJson = Json.obj(
        "websiteAddress" -> "http://example.com"
      )

      Json.toJson(website) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "websiteAddress" -> "http://example.com"
      )

      val expectedWebsite = EtmpWebsite("http://example.com")

      json.validate[EtmpWebsite] mustBe JsSuccess(expectedWebsite)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[EtmpWebsite] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "websiteAddress" -> 12345
      )

      json.validate[EtmpWebsite] mustBe a[JsError]
    }
  }
}
