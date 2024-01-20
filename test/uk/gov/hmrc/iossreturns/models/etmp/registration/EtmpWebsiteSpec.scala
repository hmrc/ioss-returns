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
import play.api.libs.json.{Json, JsSuccess}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpWebsiteSpec extends SpecBase {

  "EtmpWebsite" - {

    "must serialise/deserialise to and from EtmpWebsite" in {

      val website = arbitrary[EtmpWebsite].sample.value

      val expectedJson = Json.obj(
        "websiteAddress" -> s"${website.websiteAddress}"
      )

      Json.toJson(website) mustBe expectedJson
      expectedJson.validate[EtmpWebsite] mustBe JsSuccess(website)
    }
  }
}
