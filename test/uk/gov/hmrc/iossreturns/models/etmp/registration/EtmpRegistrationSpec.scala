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
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpDisplayRegistration._
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData.etmpRegistration

class EtmpRegistrationSpec extends SpecBase {

  private val tradingNames: Seq[EtmpTradingName] = etmpRegistration.tradingNames
  private val schemeDetails: EtmpDisplaySchemeDetails = etmpRegistration.schemeDetails
  private val bankDetails: EtmpBankDetails = etmpRegistration.bankDetails.get
  private val exclusions: Seq[EtmpExclusion] = etmpRegistration.exclusions
  private val adminUse: EtmpAdminUse = etmpRegistration.adminUse

  "EtmpRegistration" - {

    "must serialise/deserialise to and from EtmpRegistration" in {

      val json = Json.obj(
          "tradingNames" -> tradingNames,
          "schemeDetails" -> schemeDetails,
          "bankDetails" -> bankDetails,
          "exclusions" -> exclusions,
          "adminUse" -> adminUse
      )

      val expectedResult = EtmpDisplayRegistration(
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = Some(bankDetails),
        exclusions = exclusions,
        adminUse = adminUse
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpDisplayRegistration] mustBe JsSuccess(expectedResult)
    }
  }
}
