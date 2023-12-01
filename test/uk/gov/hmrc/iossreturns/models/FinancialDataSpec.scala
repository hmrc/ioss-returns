package uk.gov.hmrc.iossreturns.models

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.connectors.FinancialDataConnectorFixture
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}

import java.time.ZonedDateTime

class FinancialDataSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {
  private val zonedNow = ZonedDateTime.now()
  private val financialDataJson =
    s"""{
       | "idType": "VRN",
       | "idNumber": "123456789",
       | "regimeType": "ECOM",
       | "processingDate": "${zonedNow.toString}",
       | "financialTransactions": [
       |   {
       |     "chargeType": "G Ret AT EU-OMS",
       |     "taxPeriodFrom": "${FinancialDataConnectorFixture.dateFrom}",
       |     "taxPeriodTo": "${FinancialDataConnectorFixture.dateTo}",
       |     "originalAmount": 1000,
       |     "outstandingAmount": 500,
       |     "clearedAmount": 500,
       |     "items": [
       |       {
       |         "amount": 500,
       |         "clearingReason": "",
       |         "paymentReference": "",
       |         "paymentAmount": 500,
       |         "paymentMethod": ""
       |       }
       |     ]
       |   }
       | ]
       |}""".stripMargin

  private val item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))

  private val financialTransaction = FinancialTransaction(Some("G Ret AT EU-OMS"), None, Some(FinancialDataConnectorFixture.dateFrom), Some(FinancialDataConnectorFixture.dateTo), Some(1000), Some(500), Some(500), Some(Seq(item)))

  private val financialData = FinancialData(Some("VRN"), Some("123456789"), Some("ECOM"), zonedNow, Some(Seq(financialTransaction)))

  "FinancialData" - {
    "must deserialise correctly" in {
      Json.parse(financialDataJson).as[FinancialData] mustBe financialData
    }

    "must serialise correctly`" in {
      Json.toJson(financialData) mustBe Json.parse(financialDataJson)
    }
  }
}
