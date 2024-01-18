package uk.gov.hmrc.iossreturns.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpObligationsSpec extends SpecBase {

  private val obligationDetails: Seq[EtmpObligationDetails] = arbitraryObligations.arbitrary.sample.value.obligations.head.obligationDetails


  "EtmpObligations" - {

    "must deserialise/serialise to and from EtmpObligations" in {

      val json = Json.obj(
        "obligations" -> Json.arr(
          Json.obj(
            "obligationDetails" -> obligationDetails.map { obligationDetail =>
              Json.obj(
                "status" -> obligationDetail.status,
                "periodKey" -> obligationDetail.periodKey
              )
            }
          )
        )
      )

      val expectedResult = EtmpObligations(obligations = Seq(EtmpObligation(
        obligationDetails = obligationDetails
      )))

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligations] mustBe JsSuccess(expectedResult)
    }
  }
}
