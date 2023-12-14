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

class EtmpBankDetailsSpec extends SpecBase {

  private val accountName = arbitrary[String].sample.value
  private val genBic = arbitraryBic.arbitrary.sample.value
  private val genIban = arbitraryIban.arbitrary.sample.value

  "must deserialise/serialise to and from EtmpBankDetails" - {

    "when all optional values are present" in {

      val json = Json.obj(
        "accountName" -> accountName,
        "bic" -> genBic,
        "iban" -> genIban
      )

      val expectedResult = EtmpBankDetails(
        accountName = accountName,
        bic = Some(genBic),
        iban = genIban
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpBankDetails] mustBe JsSuccess(expectedResult)
    }

    "when all optional values are absent" in {

      val json = Json.obj(
        "accountName" -> accountName,
        "iban" -> genIban
      )

      val expectedResult = EtmpBankDetails(
        accountName = accountName,
        bic = None,
        iban = genIban
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpBankDetails] mustBe JsSuccess(expectedResult)
    }
  }
}

