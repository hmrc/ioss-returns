package uk.gov.hmrc.iossreturns.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase

class EtmpVatRateTypeSpec extends SpecBase with ScalaCheckPropertyChecks {

  "EtmpVatRateType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(EtmpVatRateType.values)

      forAll(gen) {
        etmpVatRateType =>

          JsString(etmpVatRateType.toString)
            .validate[EtmpVatRateType].asOpt.value mustBe etmpVatRateType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!EtmpVatRateType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[EtmpVatRateType] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(EtmpVatRateType.values)

      forAll(gen) {
        etmpVatRateType =>

          Json.toJson(etmpVatRateType) mustBe JsString(etmpVatRateType.toString)
      }
    }
  }
}
