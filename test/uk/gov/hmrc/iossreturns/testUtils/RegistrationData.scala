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

package uk.gov.hmrc.iossreturns.testUtils

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.config.Constants.maxTradingNames
import uk.gov.hmrc.iossreturns.models.etmp.registration._
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter

import java.time.{LocalDate, LocalDateTime}

object RegistrationData extends SpecBase {

  val etmpEuRegistrationDetails: EtmpDisplayEuRegistrationDetails = arbitrary[EtmpDisplayEuRegistrationDetails].sample.value

  val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = arbitrary[EtmpPreviousEuRegistrationDetails].sample.value

  val etmpSchemeDetails: EtmpDisplaySchemeDetails = EtmpDisplaySchemeDetails(
    commencementDate = LocalDate.now.format(etmpDateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    previousEURegistrationDetails = Seq(etmpEuPreviousRegistrationDetails),
    websites = Seq(arbitrary[EtmpWebsite].sample.value),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    unusableStatus = false,
    nonCompliantReturns = Some(arbitraryNonCompliantDetails.arbitrary.sample.value.nonCompliantReturns.toString),
    nonCompliantPayments = Some(arbitraryNonCompliantDetails.arbitrary.sample.value.nonCompliantPayments.toString)
  )

  val etmpBankDetails: EtmpBankDetails = arbitrary[EtmpBankDetails].sample.value

  val etmpAdminUse: EtmpAdminUse = EtmpAdminUse(
    changeDate = Some(LocalDateTime.now(stubClockAtArbitraryDate))
  )

  val etmpRegistration: EtmpDisplayRegistration = EtmpDisplayRegistration(
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    schemeDetails = etmpSchemeDetails,
    bankDetails = etmpBankDetails,
    exclusions = Gen.listOfN(3, arbitrary[EtmpExclusion]).sample.value,
    adminUse = etmpAdminUse
  )
}

