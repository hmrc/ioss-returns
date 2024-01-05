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
import uk.gov.hmrc.iossreturns.models.des.VatCustomerInfo
import uk.gov.hmrc.iossreturns.models.etmp._
import uk.gov.hmrc.iossreturns.models.{Bic, Country, EtmpRegistration, Iban, RegistrationWrapper}
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter

import java.time.{LocalDate, LocalDateTime}

object RegistrationData extends SpecBase {

  val etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now.format(etmpDateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    previousEURegistrationDetails = Seq(etmpEuPreviousRegistrationDetails),
    websites = Seq(arbitrary[EtmpWebsite].sample.value),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    nonCompliantReturns = Some(arbitraryNonCompliantDetails.arbitrary.sample.value.nonCompliantReturns.toString),
    nonCompliantPayments = Some(arbitraryNonCompliantDetails.arbitrary.sample.value.nonCompliantPayments.toString)
  )

  val genBankDetails: EtmpBankDetails = EtmpBankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )

  val etmpAdminUse: EtmpAdminUse = EtmpAdminUse(
    changeDate = Some(LocalDateTime.now())
  )

  val etmpRegistration: EtmpRegistration = EtmpRegistration(
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails,
    exclusions = Gen.listOfN(3, arbitrary[EtmpExclusion]).sample.value,
    adminUse = etmpAdminUse
  )

  val etmpDisplayRegistration: EtmpDisplayRegistration = EtmpDisplayRegistration(
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails,
    exclusions = Gen.listOfN(3, arbitrary[EtmpExclusion]).sample.value,
    adminUse = etmpAdminUse
  )

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      desAddress = arbitraryDesAddress.arbitrary.sample.value,
      registrationDate = Some(LocalDate.now(stubClockAtArbitraryDate)),
      partOfVatGroup = false,
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None,
      overseasIndicator = false
    )

  val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)
}

