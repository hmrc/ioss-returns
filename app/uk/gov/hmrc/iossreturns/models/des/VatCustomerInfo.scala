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

package uk.gov.hmrc.iossreturns.models.des

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, __}
import uk.gov.hmrc.iossreturns.models.DesAddress
import uk.gov.hmrc.iossreturns.models.des.PartyType.VatGroup

import java.time.LocalDate

case class VatCustomerInfo(
                            desAddress: DesAddress,
                            registrationDate: Option[LocalDate],
                            partOfVatGroup: Boolean,
                            organisationName: Option[String],
                            individualName: Option[String],
                            singleMarketIndicator: Boolean,
                            deregistrationDecisionDate: Option[LocalDate],
                            overseasIndicator: Boolean
                          )

object VatCustomerInfo {

  private def fromDesPayload(
                              address: DesAddress,
                              registrationDate: Option[LocalDate],
                              partyType: Option[PartyType],
                              organisationName: Option[String],
                              individualFirstName: Option[String],
                              individualMiddleName: Option[String],
                              individualLastName: Option[String],
                              singleMarketIndicator: Boolean,
                              deregistrationDecisionDate: Option[LocalDate],
                              overseasIndicator: Boolean
                            ): VatCustomerInfo = {

    val firstName = individualFirstName.fold("")(fn => s"$fn ")
    val middleName = individualMiddleName.fold("")(mn => s"$mn ")
    val lastName = individualLastName.fold("")(ln => s"$ln")

    VatCustomerInfo(
      desAddress = address,
      registrationDate = registrationDate,
      partOfVatGroup = partyType match {
        case Some(VatGroup) => true
        case _ => false
      },
      organisationName = organisationName,
      individualName = if(individualFirstName.isEmpty && individualMiddleName.isEmpty && individualLastName.isEmpty) {
        None
      } else {
        Some(s"$firstName$middleName$lastName")
      },
      singleMarketIndicator = singleMarketIndicator,
      deregistrationDecisionDate = deregistrationDecisionDate,
      overseasIndicator = overseasIndicator
    )
  }
//
//  implicit val desReads: Reads[VatCustomerInfo] =
//    (
//      (__ \ "approvedInformation" \ "PPOB" \ "address").read[DesAddress] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "effectiveRegistrationDate").readNullable[LocalDate] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "partyType").readNullable[PartyType] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "organisationName").readNullable[String] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "individual" \ "firstName").readNullable[String] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "individual" \ "middleName").readNullable[String] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "individual" \ "lastName").readNullable[String] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "singleMarketIndicator").read[Boolean] and
//        (__ \ "approvedInformation" \ "deregistration" \ "effectDateOfCancellation").readNullable[LocalDate] and
//        (__ \ "approvedInformation" \ "customerDetails" \ "overseasIndicator").read[Boolean]
//      ) (VatCustomerInfo.fromDesPayload _)

  implicit val writes: OWrites[VatCustomerInfo] =
    Json.writes[VatCustomerInfo]

  implicit val reads: Reads[VatCustomerInfo] =
    Json.reads[VatCustomerInfo]


}
