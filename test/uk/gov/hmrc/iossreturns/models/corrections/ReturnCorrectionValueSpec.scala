package uk.gov.hmrc.iossreturns.models.corrections

import play.api.libs.json.{JsError, JsSuccess, Json}
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

    "must serialise/deserialise to/from JSON correctly" in {
      val json = Json.obj(
        "maximumCorrectionValue" -> returnCorrectionValueResponse.maximumCorrectionValue
      )

      val expectedResult: ReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = returnCorrectionValueResponse.maximumCorrectionValue
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedResult)
    }

    "must fail to deserialise when JSON is missing the required field" in {
      val json = Json.obj()

      json.validate[ReturnCorrectionValue] mustBe a[JsError]
    }

    "must fail to deserialise when JSON has an invalid type for maximumCorrectionValue" in {
      val json = Json.obj(
        "maximumCorrectionValue" -> "invalidType" // Using a string instead of a BigDecimal
      )

      json.validate[ReturnCorrectionValue] mustBe a[JsError]
    }

    "must deserialise when the maximumCorrectionValue is zero" in {
      val json = Json.obj(
        "maximumCorrectionValue" -> BigDecimal(0)
      )

      val expectedResult: ReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = BigDecimal(0)
      )

      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedResult)
      Json.toJson(expectedResult) mustBe json
    }

    "must deserialise when the maximumCorrectionValue is a negative value" in {
      val json = Json.obj(
        "maximumCorrectionValue" -> BigDecimal(-100.50)
      )

      val expectedResult: ReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = BigDecimal(-100.50)
      )

      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedResult)
      Json.toJson(expectedResult) mustBe json
    }

    "must serialise/deserialise with large numbers for maximumCorrectionValue" in {
      val largeValue = BigDecimal("1000000000000000000000000.9999")
      val json = Json.obj(
        "maximumCorrectionValue" -> largeValue
      )

      val expectedResult: ReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = largeValue
      )

      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedResult)
      Json.toJson(expectedResult) mustBe json
    }

    "must serialise/deserialise to/from JSON with extra fields ignored" in {
      val json = Json.obj(
        "maximumCorrectionValue" -> returnCorrectionValueResponse.maximumCorrectionValue,
        "extraField" -> "extraValue"
      )

      val expectedResult: ReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = returnCorrectionValueResponse.maximumCorrectionValue
      )

      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedResult)
    }
  }
}
