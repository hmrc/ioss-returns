package uk.gov.hmrc.iossreturns.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpObligationDetailsSpec extends SpecBase {

  private val etmpObligationDetails: EtmpObligationDetails = arbitraryObligationDetails.arbitrary.sample.value

  "EtmpObligationDetails" - {

    "must deserialise/serialise to and from EtmpObligationDetails" in {

      val json = Json.obj(
        "status" -> etmpObligationDetails.status,
        "periodKey" -> etmpObligationDetails.periodKey
      )

      val expectedResult = EtmpObligationDetails(
        status = etmpObligationDetails.status,
        periodKey = etmpObligationDetails.periodKey
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligationDetails] mustBe JsSuccess(expectedResult)
    }
  }
}
