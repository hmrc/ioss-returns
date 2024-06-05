package uk.gov.hmrc.iossreturns.models.corrections

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class ReturnCorrectionValueSpec extends SpecBase {

  private val returnCorrectionValueResponse: ReturnCorrectionValue = arbitraryReturnCorrectionValue.arbitrary.sample.value
  "ReturnCorrectionValue" - {

    "must serialise/deserialise to/from a ReturnCorrectionValue" in {

      val json = Json.obj(
        "maximumCorrectionValue" -> returnCorrectionValueResponse.maximumCorrectionValue
      )

      val expectedResult: ReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = returnCorrectionValueResponse.maximumCorrectionValue
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedResult)
    }
  }
}
