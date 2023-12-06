package uk.gov.hmrc.iossreturns.models

import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.FinancialDataConnectorFixture
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}

import java.time.ZonedDateTime

class FinancialDataSpec extends SpecBase with FinancialDataConnectorFixture {

  private val zonedDateTimeNow = ZonedDateTime.now()

  private val financialDataJson =
    s"""{
       | "idType": "IOSS",
       | "idNumber": "123456789",
       | "regimeType": "ECOM",
       | "processingDate": "${zonedDateTimeNow.toString}",
       | "financialTransactions": [
       |   {
       |     "chargeType": "G Ret AT EU-OMS",
       |     "taxPeriodFrom": "${dateFrom}",
       |     "taxPeriodTo": "${dateTo}",
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

  private val financialTransaction = FinancialTransaction(Some("G Ret AT EU-OMS"), None, Some(dateFrom), Some(dateTo), Some(1000), Some(500), Some(500), Some(Seq(item)))

  private val financialData = FinancialData(Some("IOSS"), Some("123456789"), Some("ECOM"), zonedDateTimeNow, Some(Seq(financialTransaction)))

  "FinancialData" - {
    "must deserialise correctly" in {
      Json.parse(financialDataJson).as[FinancialData] mustBe financialData
    }

    "must serialise correctly`" in {
      Json.toJson(financialData) mustBe Json.parse(financialDataJson)
    }
  }
}
