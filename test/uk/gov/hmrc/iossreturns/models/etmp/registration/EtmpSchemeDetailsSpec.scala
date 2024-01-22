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

import play.api.libs.json.{Json, JsSuccess}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData.etmpSchemeDetails

class EtmpSchemeDetailsSpec extends SpecBase {

  private val commencementDate = etmpSchemeDetails.commencementDate
  private val euRegistrationDetails = etmpSchemeDetails.euRegistrationDetails
  private val previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails
  private val websites = etmpSchemeDetails.websites
  private val contactName = etmpSchemeDetails.contactName
  private val businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber
  private val businessEmailId = etmpSchemeDetails.businessEmailId
  private val nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns
  private val nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments

  "EtmpSchemeDetails" - {

    "must deserialise/serialise to and from EtmpSchemeDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "commencementDate" -> commencementDate,
          "euRegistrationDetails" -> euRegistrationDetails,
          "previousEURegistrationDetails" -> previousEURegistrationDetails,
          "websites" -> websites,
          "contactName" -> contactName,
          "businessTelephoneNumber" -> businessTelephoneNumber,
          "businessEmailId" -> businessEmailId,
          "unusableStatus" -> false,
          "nonCompliantReturns" -> nonCompliantReturns,
          "nonCompliantPayments" -> nonCompliantPayments
        )

        val expectedResult = EtmpDisplaySchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          previousEURegistrationDetails = previousEURegistrationDetails,
          websites = websites,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          unusableStatus = false,
          nonCompliantReturns = nonCompliantReturns,
          nonCompliantPayments = nonCompliantPayments
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpDisplaySchemeDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "commencementDate" -> commencementDate,
          "euRegistrationDetails" -> euRegistrationDetails,
          "previousEURegistrationDetails" -> previousEURegistrationDetails,
          "websites" -> websites,
          "contactName" -> contactName,
          "businessTelephoneNumber" -> businessTelephoneNumber,
          "businessEmailId" -> businessEmailId,
          "unusableStatus" -> false
        )

        val expectedResult = EtmpDisplaySchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          previousEURegistrationDetails = previousEURegistrationDetails,
          websites = websites,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          unusableStatus = false,
          nonCompliantReturns = None,
          nonCompliantPayments = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpDisplaySchemeDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}

