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

package uk.gov.hmrc.iossreturns.models.etmp.registration

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.libs.json.{Json, JsSuccess}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData.etmpEuRegistrationDetails

class EtmpEuRegistrationDetailsSpec extends SpecBase {

  private val issuedBy = etmpEuRegistrationDetails.issuedBy
  private val vatNumber = etmpEuRegistrationDetails.vatNumber
  private val taxIdentificationNumber = Gen.alphaNumStr.sample.value
  private val tradingName = etmpEuRegistrationDetails.fixedEstablishmentTradingName
  private val fixedEstablishmentAddressLine1 = etmpEuRegistrationDetails.fixedEstablishmentAddressLine1
  private val fixedEstablishmentAddressLine2 = arbitrary[String].sample.value
  private val townOrCity = etmpEuRegistrationDetails.townOrCity
  private val regionOrState = arbitrary[String].sample.value
  private val postcode = arbitrary[String].sample.value

  "EtmpEuRegistrationDetails" - {

    "must deserialise/serialise to and from EtmpEuRegistrationDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "issuedBy" -> issuedBy,
          "vatNumber" -> vatNumber.get,
          "taxIdentificationNumber" -> taxIdentificationNumber,
          "fixedEstablishmentTradingName" -> tradingName,
          "fixedEstablishmentAddressLine1" -> fixedEstablishmentAddressLine1,
          "fixedEstablishmentAddressLine2" -> fixedEstablishmentAddressLine2,
          "townOrCity" -> townOrCity,
          "regionOrState" -> regionOrState,
          "postcode" -> postcode
        )

        val expectedResult = EtmpDisplayEuRegistrationDetails(
          issuedBy = issuedBy,
          vatNumber = vatNumber,
          taxIdentificationNumber = Some(taxIdentificationNumber),
          fixedEstablishmentTradingName = tradingName,
          fixedEstablishmentAddressLine1 = fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = Some(fixedEstablishmentAddressLine2),
          townOrCity = townOrCity,
          regionOrState = Some(regionOrState),
          postcode = Some(postcode)
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpDisplayEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "issuedBy" -> issuedBy,
          "vatNumber" -> vatNumber,
          "fixedEstablishmentTradingName" -> tradingName,
          "fixedEstablishmentAddressLine1" -> fixedEstablishmentAddressLine1,
          "townOrCity" -> townOrCity
        )

        val expectedResult = EtmpDisplayEuRegistrationDetails(
          issuedBy = issuedBy,
          vatNumber = vatNumber,
          taxIdentificationNumber = None,
          fixedEstablishmentTradingName = tradingName,
          fixedEstablishmentAddressLine1 = fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = None,
          townOrCity = townOrCity,
          regionOrState = None,
          postcode = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpDisplayEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}

