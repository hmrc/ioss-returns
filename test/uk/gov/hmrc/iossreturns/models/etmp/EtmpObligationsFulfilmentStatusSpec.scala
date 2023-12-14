package uk.gov.hmrc.iossreturns.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpObligationsFulfilmentStatusSpec extends SpecBase with ScalaCheckPropertyChecks {

  "EtmpObligationsFulfilmentStatus" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(EtmpObligationsFulfilmentStatus.values)

      forAll(gen) {
        obligationFulfilmentStatus =>

          JsString(obligationFulfilmentStatus.toString)
            .validate[EtmpObligationsFulfilmentStatus].asOpt.value mustBe obligationFulfilmentStatus
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!EtmpObligationsFulfilmentStatus.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[EtmpObligationsFulfilmentStatus] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(EtmpObligationsFulfilmentStatus.values)

      forAll(gen) {
        obligationFulfilmentStatus =>

          Json.toJson(obligationFulfilmentStatus) mustBe JsString(obligationFulfilmentStatus.toString)
      }
    }
  }
}
