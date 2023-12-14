package uk.gov.hmrc.iossreturns.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpObligationsSpec extends SpecBase {

  private val referenceNumber: String = arbitraryObligations.arbitrary.sample.value.referenceNumber
  private val referenceType: String = arbitraryObligations.arbitrary.sample.value.referenceType
  private val obligationDetails: Seq[EtmpObligationDetails] = arbitraryObligations.arbitrary.sample.value.obligationDetails


  "EtmpObligations" - {

    "must deserialise/serialise to and from EtmpObligations" in {

      val json = Json.obj(
        "referenceNumber" -> referenceNumber,
        "referenceType" -> referenceType,
        "obligationDetails" -> obligationDetails.map { obligationDetail =>
          Json.obj(
            "status" -> obligationDetail.status,
            "periodKey" -> obligationDetail.periodKey
          )
        }
      )

      val expectedResult = EtmpObligations(
        referenceNumber = referenceNumber,
        referenceType = referenceType,
        obligationDetails = obligationDetails
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligations] mustBe JsSuccess(expectedResult)
    }
  }
}
