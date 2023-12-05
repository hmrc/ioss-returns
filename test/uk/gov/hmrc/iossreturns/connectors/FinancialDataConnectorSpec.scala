package uk.gov.hmrc.iossreturns.connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import connectors.WireMockHelper
import org.scalatest.OptionValues
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.generators.ModelGenerators
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialDataErrorResponse, FinancialData, FinancialDataQueryParameters, FinancialTransaction, Item, UnexpectedResponseStatus}

import java.time.{LocalDate, ZonedDateTime}

class FinancialDataConnectorSpec extends SpecBase with WireMockHelper {
  def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.financial-data.host" -> "127.0.0.1",
        "microservice.services.financial-data.port" -> server.port,
        "microservice.services.financial-data.authorizationToken" -> "auth-token",
        "microservice.services.financial-data.environment" -> "test-environment",
        "microservice.services.financial-data.regimeType" -> "ECOM"
      )
      .build()
  private val now = FinancialDataConnectorFixture.zonedNow.toLocalDate
  "getFinancialData" - {

    "when the server returns OK and a recognised payload" - {
      "must return a FinancialDataResponse" in {
        val app = application

        server.stubFor(
          get(urlEqualTo(s"${FinancialDataConnectorFixture.financialDataUrl}?dateFrom=${FinancialDataConnectorFixture.dateFrom.toString}&dateTo=${FinancialDataConnectorFixture.queryParameters.toDate.get.toString}"))
            .withQueryParam("dateFrom", new EqualToPattern(FinancialDataConnectorFixture.dateFrom.toString))
            .withQueryParam("dateTo", new EqualToPattern(now.toString))
            .withHeader("Authorization", equalTo("Bearer auth-token"))
            .withHeader("Environment", equalTo("test-environment"))
            .willReturn(ok(FinancialDataConnectorFixture.responseJson))
        )

        running(app) {
          val connector = app.injector.instanceOf[FinancialDataConnector]
          val result = connector.getFinancialData(FinancialDataConnectorFixture.iossNumber, FinancialDataConnectorFixture.queryParameters).futureValue

          result mustEqual FinancialDataConnectorFixture.expectedResult
        }
      }
    }

    "must return None when server returns Not Found" in {
      server.stubFor(
        get(urlEqualTo(s"${FinancialDataConnectorFixture.financialDataUrl}?dateFrom=${FinancialDataConnectorFixture.dateFrom}&dateTo=${FinancialDataConnectorFixture.queryParameters.toDate.get.toString}"))
          .withQueryParam("dateFrom", new EqualToPattern(FinancialDataConnectorFixture.dateFrom.toString))
          .withQueryParam("dateTo", new EqualToPattern(now.toString))
          .withHeader("Authorization", equalTo("Bearer auth-token"))
          .withHeader("Environment", equalTo("test-environment"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]
        val result = connector.getFinancialData(FinancialDataConnectorFixture.iossNumber, FinancialDataConnectorFixture.queryParameters).futureValue
        result mustBe Right(None)
      }

    }

    "must return FinancialDataErrorResponse" - {
      "when server returns Http Exception" in {
        server.stubFor(
          get(urlEqualTo(s"${FinancialDataConnectorFixture.financialDataUrl}?dateFrom=${FinancialDataConnectorFixture.dateFrom.toString}&dateTo=${FinancialDataConnectorFixture.queryParameters.toDate.get.toString}"))
            .withQueryParam("dateFrom", new EqualToPattern(FinancialDataConnectorFixture.dateFrom.toString))
            .withQueryParam("dateTo", new EqualToPattern(now.toString))
            .withHeader("Authorization", equalTo("Bearer auth-token"))
            .withHeader("Environment", equalTo("test-environment"))
            .willReturn(
              aResponse()
                .withStatus(504)
                .withFixedDelay(21000)
            )
        )

        running(application) {
          val connector = application.injector.instanceOf[FinancialDataConnector]
          whenReady(connector.getFinancialData(FinancialDataConnectorFixture.iossNumber, FinancialDataConnectorFixture.queryParameters), Timeout(Span(30, Seconds))) { exp =>
            exp.isLeft mustBe true
            exp.left.toOption.get mustBe a[FinancialDataErrorResponse]
          }

        }
      }

      Seq(BAD_REQUEST, SERVICE_UNAVAILABLE, IM_A_TEAPOT).foreach {
        status =>
          s"when server returns status $status" in {
            println("---" + s"${FinancialDataConnectorFixture.financialDataUrl}?dateFrom=${FinancialDataConnectorFixture.dateFrom.toString}&dateTo=${FinancialDataConnectorFixture.queryParameters.toDate.get.toString}")
            server.stubFor(
              get(urlEqualTo(s"${FinancialDataConnectorFixture.financialDataUrl}?dateFrom=${FinancialDataConnectorFixture.dateFrom.toString}&dateTo=${FinancialDataConnectorFixture.queryParameters.toDate.get.toString}"))
                .withQueryParam("dateFrom", new EqualToPattern(FinancialDataConnectorFixture.dateFrom.toString))
                .withQueryParam("dateTo", new EqualToPattern(now.toString))
                .withHeader("Authorization", equalTo("Bearer auth-token"))
                .withHeader("Environment", equalTo("test-environment"))
                .willReturn(
                  aResponse()
                    .withStatus(status)
                )
            )

            running(application) {
              val connector = application.injector.instanceOf[FinancialDataConnector]
              val result = connector.getFinancialData(FinancialDataConnectorFixture.iossNumber, FinancialDataConnectorFixture.queryParameters).futureValue
              result mustBe Left(UnexpectedResponseStatus(status, s"Unexpected response from Financial Data, received status $status"))
            }
          }
      }

    }
  }

}

object FinancialDataConnectorFixture extends ModelGenerators with OptionValues {
  val iossNumber = arbitraryIOSSNumber.arbitrary.sample.value
  val financialDataUrl = s"/ioss-returns-stub/enterprise/financial-data/IOSS/${iossNumber.value}/ECOM"
  val dateFrom = LocalDate.now().minusMonths(1)
  val dateTo = LocalDate.now()
  val queryParameters = FinancialDataQueryParameters(fromDate = Some(dateFrom), toDate = Some(dateTo))

  val zonedNow = ZonedDateTime.now()

  val responseJson =
    s"""{
       | "idType": "IOSS",
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

  private val items = Seq(
    Item(
      amount = Some(BigDecimal(500)),
      clearingReason = Some(""),
      paymentReference = Some(""),
      paymentAmount = Some(BigDecimal(500)),
      paymentMethod = Some("")
    )
  )

  val financialTransactions = Seq(
    FinancialTransaction(
      chargeType = Some("G Ret AT EU-OMS"),
      mainType = None,
      taxPeriodFrom = Some(FinancialDataConnectorFixture.dateFrom),
      taxPeriodTo = Some(FinancialDataConnectorFixture.dateTo),
      originalAmount = Some(BigDecimal(1000)),
      outstandingAmount = Some(BigDecimal(500)),
      clearedAmount = Some(BigDecimal(500)),
      items = Some(items)
    )
  )

  val expectedResult = Right(Some(FinancialData(
    idType = Some("IOSS"),
    idNumber = Some("123456789"),
    regimeType = Some("ECOM"),
    processingDate = zonedNow,
    financialTransactions = Option(financialTransactions))))
}