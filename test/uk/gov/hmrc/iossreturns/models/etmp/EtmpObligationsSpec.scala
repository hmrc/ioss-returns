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

    "must deserialise/serialise the example and from EtmpObligations" in {

      val json = Json.parse("""{
                              |  "obligations": [
                              |    {
                              |      "identification": {
                              |        "incomeSourceType": "ITSA",
                              |        "referenceNumber": "AB123456A",
                              |        "referenceType": "NINO"
                              |      },
                              |      "obligationDetails": [
                              |        {
                              |          "status": "O",
                              |          "inboundCorrespondenceFromDate": "1920-02-29",
                              |          "inboundCorrespondenceToDate": "1920-02-29",
                              |          "inboundCorrespondenceDateReceived": "1920-02-29",
                              |          "inboundCorrespondenceDueDate": "1920-02-29",
                              |          "periodKey": "#001"
                              |        },
                              |        {
                              |          "status": "O",
                              |          "inboundCorrespondenceFromDate": "1920-02-29",
                              |          "inboundCorrespondenceToDate": "1920-02-29",
                              |          "inboundCorrespondenceDateReceived": "1920-02-29",
                              |          "inboundCorrespondenceDueDate": "1920-02-29",
                              |          "periodKey": "#001"
                              |        }
                              |      ]
                              |    }
                              |  ]
                              |}""".stripMargin)

      val expectedInternalJson = Json.parse(
        """{
          |  "obligations": [
          |    {
          |      "obligationDetails": [
          |        {
          |          "status": "O",
          |          "periodKey": "#001"
          |        },
          |        {
          |          "status": "O",
          |          "periodKey": "#001"
          |        }
          |      ]
          |    }
          |  ]
          |}""".stripMargin)

      val expectedResult = EtmpObligations(obligations = Seq(EtmpObligation(
        obligationDetails = Seq(
          EtmpObligationDetails(EtmpObligationsFulfilmentStatus.Open, "#001"),
          EtmpObligationDetails(EtmpObligationsFulfilmentStatus.Open, "#001")
        )
      )))

      expectedInternalJson mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligations] mustBe JsSuccess(expectedResult)
    }
  }
}
