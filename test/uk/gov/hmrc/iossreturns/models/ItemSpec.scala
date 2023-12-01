package uk.gov.hmrc.iossreturns.models

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.models.financialdata.Item
import uk.gov.hmrc.iossreturns.models.financialdata.Item._
class ItemSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {
  private val itemJson = """{
               |         "amount": 500,
               |         "clearingReason": "",
               |         "paymentReference": "",
               |         "paymentAmount": 500,
               |         "paymentMethod": ""
               |       }""".stripMargin

  private val item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))

  "Item" - {
    "must deserialise correctly" in {
      Json.parse(itemJson).as[Item] mustBe item
    }

    "must serialise correctly`" in {
      Json.toJson(item) mustBe Json.parse(itemJson)
    }
  }
}
