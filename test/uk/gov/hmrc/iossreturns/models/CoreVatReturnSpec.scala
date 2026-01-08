/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.iossreturns.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.UUID

class CoreVatReturnSpec extends SpecBase with Matchers {

  val vatId: CoreEuTraderVatId = CoreEuTraderVatId("VAT123456", "DE")
  val taxId: CoreEuTraderTaxId = CoreEuTraderTaxId("TAX987654", "FR")

  "CoreVatReturn" - {

    "serialize and deserialize correctly" in {
      val serializedJson = Json.toJson(coreVatReturn)
      val deserializedObject = serializedJson.as[CoreVatReturn]

      deserializedObject mustEqual coreVatReturn.copy(
        changeDate = coreVatReturn.changeDate.map(_.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime)
      )
    }

    "ensure all fields are serialized correctly" in {
      val json = Json.toJson(coreVatReturn)

      (json \ "vatReturnReferenceNumber").as[String] mustEqual coreVatReturn.vatReturnReferenceNumber
      (json \ "version").as[Instant] mustEqual coreVatReturn.version
      (json \ "traderId" \ "IOSSNumber").as[String] mustEqual coreVatReturn.traderId.IOSSNumber
      (json \ "period" \ "year").as[Int] mustEqual coreVatReturn.period.year
      (json \ "period" \ "month").as[String] mustEqual coreVatReturn.period.month
      (json \ "startDate").as[LocalDate] mustEqual coreVatReturn.startDate
      (json \ "endDate").as[LocalDate] mustEqual coreVatReturn.endDate
      (json \ "submissionDateTime").as[Instant] mustEqual coreVatReturn.submissionDateTime
      (json \ "totalAmountVatDueGBP").as[BigDecimal] mustEqual coreVatReturn.totalAmountVatDueGBP

      (json \ "msconSupplies").as[List[JsObject]].headOption.map { msconJson =>
        (msconJson \ "msconCountryCode").as[String] mustEqual "DE"
        (msconJson \ "balanceOfVatDueGBP").as[BigDecimal] mustEqual BigDecimal(10)
      }.getOrElse(fail("msconSupplies list is empty"))
    }

    "handle optional changeDate field" - {

      "With changeDate" in {
        val jsonWithChangeDate = Json.toJson(coreVatReturn)
        (jsonWithChangeDate \ "changeDate").asOpt[LocalDateTime] mustBe defined
      }

      "Without changeDate" in {
        val vatReturnWithoutChangeDate = coreVatReturn.copy(changeDate = None)
        val jsonWithoutChangeDate = Json.toJson(vatReturnWithoutChangeDate)
        (jsonWithoutChangeDate \ "changeDate").isDefined mustBe false
      }
    }

    "test invalid JSON" in {
      val invalidJson = Json.obj(
        "vatReturnReferenceNumber" -> "XI/XI063407423/M11.2086",
        "version" -> "Invalid version"
      )

      val result = invalidJson.asOpt[CoreVatReturn]
      result mustBe None
    }

    "serialize and deserialize CoreVatReturn with empty msconSupplies" in {
      val vatReturnWithEmptySupplies = coreVatReturn.copy(msconSupplies = List.empty)
      val serializedJson = Json.toJson(vatReturnWithEmptySupplies)
      val deserializedObject = serializedJson.as[CoreVatReturn]

      deserializedObject.msconSupplies mustBe empty
    }

    "handle boundary values for totalAmountVatDueGBP" in {
      val maxValue = BigDecimal("999999999999.99")
      val vatReturnWithMaxValue = coreVatReturn.copy(totalAmountVatDueGBP = maxValue)

      val serializedJson = Json.toJson(vatReturnWithMaxValue)
      (serializedJson \ "totalAmountVatDueGBP").as[BigDecimal] mustEqual maxValue
    }

    "handle missing fields in JSON" in {
      val invalidJson = Json.obj(
        "vatReturnReferenceNumber" -> "XI/XI063407423/M11.2086"
      )

      val result = invalidJson.validate[CoreVatReturn]
      result.isError mustBe true
    }

    "handle invalid field types in JSON" in {
      val invalidJson = Json.obj(
        "vatReturnReferenceNumber" -> "XI/XI063407423/M11.2086",
        "version" -> "Invalid Instant"
      )

      val result = invalidJson.validate[CoreVatReturn]
      result.isError mustBe true
    }
  }

  "CorePeriod" - {
    "correctly format the period" in {
      val period = CorePeriod(2021, "03")
      period.toString mustEqual "2021-M03"
    }
  }


  "CoreEuTraderId" - {

    "deserialize a CoreEuTraderId" in {
      val vatJson = Json.obj(
        "vatIdNumber" -> "VAT123456",
        "issuedBy" -> "DE"
      )

      val taxJson = Json.obj(
        "taxRefNumber" -> "TAX987654",
        "issuedBy" -> "FR"
      )

      val vatResult = vatJson.as[CoreEuTraderId]
      vatResult mustBe a[CoreEuTraderVatId]
      vatResult.asInstanceOf[CoreEuTraderVatId].vatIdNumber mustBe "VAT123456"
      vatResult.asInstanceOf[CoreEuTraderVatId].issuedBy mustBe "DE"

      val taxResult = taxJson.as[CoreEuTraderId]
      taxResult mustBe a[CoreEuTraderTaxId]
      taxResult.asInstanceOf[CoreEuTraderTaxId].taxRefNumber mustBe "TAX987654"
      taxResult.asInstanceOf[CoreEuTraderTaxId].issuedBy mustBe "FR"
    }

    "serialize a CoreEuTraderId" in {
      val vatJson = Json.toJson(vatId)
      (vatJson \ "vatIdNumber").as[String] mustBe "VAT123456"
      (vatJson \ "issuedBy").as[String] mustBe "DE"

      val taxJson = Json.toJson(taxId)
      (taxJson \ "taxRefNumber").as[String] mustBe "TAX987654"
      (taxJson \ "issuedBy").as[String] mustBe "FR"
    }

    "handle unknown discriminator in JSON for CoreEuTraderId" in {
      val unknownJson = Json.obj(
        "unknownField" -> "something",
        "issuedBy" -> "DE"
      )

      val result = unknownJson.validate[CoreEuTraderId]

      result.isError mustBe true
    }

    "handle empty JSON for CoreEuTraderId" in {
      val emptyJson = Json.obj()
      val result = emptyJson.validate[CoreEuTraderId]

      result.isError mustBe true
    }

    "serialize a CoreEuTraderVatId using CoreEuTraderId Writes" in {
      val serializedJson = Json.toJson(vatId)(CoreEuTraderId.writes)
      (serializedJson \ "vatIdNumber").as[String] mustBe "VAT123456"
      (serializedJson \ "issuedBy").as[String] mustBe "DE"
    }

    "serialize a CoreEuTraderTaxId using CoreEuTraderId Writes" in {
      val serializedJson = Json.toJson(taxId)(CoreEuTraderId.writes)
      (serializedJson \ "taxRefNumber").as[String] mustBe "TAX987654"
      (serializedJson \ "issuedBy").as[String] mustBe "FR"
    }
  }

  "CoreEuTraderVatId" - {

    "deserialize a CoreEuTraderVatId correctly" in {
      val json = Json.obj(
        "vatIdNumber" -> "VAT123456",
        "issuedBy" -> "DE"
      )

      val result = json.as[CoreEuTraderId]
      result mustBe a[CoreEuTraderVatId]
      result.asInstanceOf[CoreEuTraderVatId].vatIdNumber mustBe "VAT123456"
      result.asInstanceOf[CoreEuTraderVatId].issuedBy mustBe "DE"
    }

    "serialize a CoreEuTraderVatId correctly" in {
      val json = Json.toJson(vatId)
      (json \ "vatIdNumber").as[String] mustBe "VAT123456"
      (json \ "issuedBy").as[String] mustBe "DE"
    }

    "handle invalid JSON for CoreEuTraderId" in {
      val invalidJson = Json.obj("vatIdNumber" -> "VAT123456")
      val result = invalidJson.validate[CoreEuTraderId]
      result.isError mustBe true
    }
  }

  "CoreEuTraderTaxId" - {

    "deserialize a CoreEuTraderTaxId correctly" in {
      val json = Json.obj(
        "taxRefNumber" -> "TAX987654",
        "issuedBy" -> "FR"
      )

      val result = json.as[CoreEuTraderId]
      result mustBe a[CoreEuTraderTaxId]
      result.asInstanceOf[CoreEuTraderTaxId].taxRefNumber mustBe "TAX987654"
      result.asInstanceOf[CoreEuTraderTaxId].issuedBy mustBe "FR"
    }

    "serialize a CoreEuTraderTaxId correctly" in {
      val json = Json.toJson(taxId)
      (json \ "taxRefNumber").as[String] mustBe "TAX987654"
      (json \ "issuedBy").as[String] mustBe "FR"
    }

    "handle empty JSON for CoreEuTraderId" in {
      val emptyJson = Json.obj()
      val result = emptyJson.validate[CoreEuTraderId]

      result.isError mustBe true
    }

    "EisErrorResponse" - {

      "must serialize to JSON correctly" in {
        val errorResponse = EisErrorResponse(
          CoreErrorResponse(
            timestamp = Instant.parse("2023-01-01T00:00:00Z"),
            transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
            errorCode = "OSS_001",
            errorMessage = "An error occurred"
          )
        )

        val expectedJson = Json.obj(
          "errorDetail" -> Json.obj(
            "timestamp" -> "2023-01-01T00:00:00Z",
            "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
            "errorCode" -> "OSS_001",
            "errorMessage" -> "An error occurred"
          )
        )

        Json.toJson(errorResponse) mustBe expectedJson
      }

      "must deserialize from JSON correctly" in {
        val json = Json.obj(
          "errorDetail" -> Json.obj(
            "timestamp" -> "2023-01-01T00:00:00Z",
            "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
            "errorCode" -> "OSS_001",
            "errorMessage" -> "An error occurred"
          )
        )

        val expectedResponse = EisErrorResponse(
          CoreErrorResponse(
            timestamp = Instant.parse("2023-01-01T00:00:00Z"),
            transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
            errorCode = "OSS_001",
            errorMessage = "An error occurred"
          )
        )

        json.as[EisErrorResponse] mustBe expectedResponse
      }

      "must handle missing optional fields during deserialization" in {
        val json = Json.obj(
          "errorDetail" -> Json.obj(
            "timestamp" -> "2023-01-01T00:00:00Z",
            "errorCode" -> "OSS_001",
            "errorMessage" -> "An error occurred"
          )
        )

        val expectedResponse = EisErrorResponse(
          CoreErrorResponse(
            timestamp = Instant.parse("2023-01-01T00:00:00Z"),
            transactionId = None,
            errorCode = "OSS_001",
            errorMessage = "An error occurred"
          )
        )

        json.as[EisErrorResponse] mustBe expectedResponse
      }

      "must fail to deserialize when required fields are missing" in {
        val invalidJson = Json.obj(
          "errorDetail" -> Json.obj(
            "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
            "errorCode" -> "OSS_001"
          )
        )

        val result = invalidJson.validate[EisErrorResponse]
        result.isError mustBe true
      }

      "must fail to deserialize when field types are invalid" in {
        val invalidJson = Json.obj(
          "errorDetail" -> Json.obj(
            "timestamp" -> "Invalid timestamp",
            "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
            "errorCode" -> 123,
            "errorMessage" -> true
          )
        )

        val result = invalidJson.validate[EisErrorResponse]
        result.isError mustBe true
      }
    }

    "CoreCorrection" - {

      "must serialize to JSON correctly" in {
        val correction = CoreCorrection(CorePeriod(2023, "01"), BigDecimal(100.50))
        val expectedJson = Json.obj(
          "period" -> Json.obj(
            "year" -> 2023,
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> 100.50
        )

        Json.toJson(correction) mustBe expectedJson
      }

      "must deserialize from JSON correctly" in {
        val json = Json.obj(
          "period" -> Json.obj(
            "year" -> 2023,
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> 100.50
        )

        val expectedCorrection = CoreCorrection(CorePeriod(2023, "01"), BigDecimal(100.50))
        json.as[CoreCorrection] mustBe expectedCorrection
      }

      "must handle missing fields during deserialization" in {
        val invalidJson = Json.obj(
          "period" -> Json.obj(
            "year" -> 2023
          )
        )

        val result = invalidJson.validate[CoreCorrection]
        result.isError mustBe true
      }

      "must handle invalid data during deserialization" in {
        val invalidJson = Json.obj(
          "period" -> Json.obj(
            "year" -> "invalid-year",
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> "invalid-value"
        )

        val result = invalidJson.validate[CoreCorrection]
        result.isError mustBe true
      }

      "must handle boundary values for totalVatAmountCorrectionGBP" in {
        val zeroValueJson = Json.obj(
          "period" -> Json.obj(
            "year" -> 2023,
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> 0
        )
        val zeroValueCorrection = zeroValueJson.as[CoreCorrection]
        zeroValueCorrection.totalVatAmountCorrectionGBP mustBe BigDecimal(0)

        val largeValueJson = Json.obj(
          "period" -> Json.obj(
            "year" -> 2023,
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> BigDecimal("999999999.99")
        )
        val largeValueCorrection = largeValueJson.as[CoreCorrection]
        largeValueCorrection.totalVatAmountCorrectionGBP mustBe BigDecimal("999999999.99")

        val negativeValueJson = Json.obj(
          "period" -> Json.obj(
            "year" -> 2023,
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> BigDecimal("-100.50")
        )
        val negativeValueCorrection = negativeValueJson.as[CoreCorrection]
        negativeValueCorrection.totalVatAmountCorrectionGBP mustBe BigDecimal("-100.50")
      }

      "must serialize and deserialize CoreCorrection with nested CorePeriod correctly" in {
        val correction = CoreCorrection(CorePeriod(2023, "01"), BigDecimal(100.50))
        val serializedJson = Json.toJson(correction)
        val deserializedObject = serializedJson.as[CoreCorrection]

        deserializedObject mustBe correction
      }

      "must fail deserialization when period is invalid" in {
        val invalidJson = Json.obj(
          "period" -> Json.obj(
            "year" -> "invalid-year",
            "month" -> "01"
          ),
          "totalVatAmountCorrectionGBP" -> 100.50
        )

        val result = invalidJson.validate[CoreCorrection]
        result.isError mustBe true
      }
    }
  }
}
