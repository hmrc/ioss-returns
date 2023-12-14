package uk.gov.hmrc.iossreturns.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

import java.time.LocalDateTime

class EtmpAdminUseSpec extends SpecBase {

  // TODO -> needs withSecond or fails
  private val changeDate: LocalDateTime = LocalDateTime.now(stubClockAtArbitraryDate).withSecond(1)

  "EtmpAdminUse" - {

    "must serialise/deserialise to and from EtmpAdminUse" in {

      val json = Json.obj(
        "changeDate" -> s"$changeDate"
      )

      val expectedResult = EtmpAdminUse(changeDate = Some(changeDate))

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpAdminUse] mustBe JsSuccess(expectedResult)
    }

    "when all optional values are absent" in {

      val json = Json.obj()

      val expectedResult = EtmpAdminUse(
        changeDate = None
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpAdminUse] mustBe JsSuccess(expectedResult)
    }
  }
}