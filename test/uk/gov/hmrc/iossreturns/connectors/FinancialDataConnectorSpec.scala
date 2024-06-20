package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.FinancialDataHttpParser.FinancialDataResponse
import uk.gov.hmrc.iossreturns.models.financialdata._

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

class FinancialDataConnectorSpec extends SpecBase with WireMockHelper with FinancialDataConnectorFixture {
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

  private val now = zonedNow.toLocalDate
  "getFinancialData" - {
    val financialDataUrl = s"/ioss-returns-stub/enterprise/financial-data/IOSS/$iossNumber/ECOM"

    "when the server returns OK and a recognised payload" - {
      "must return a FinancialDataResponse" in {
        val app = application

        server.stubFor(
          get(urlEqualTo(s"$financialDataUrl?dateFrom=$dateFrom&dateTo=${queryParameters.toDate.value}"))
            .withQueryParam("dateFrom", new EqualToPattern(dateFrom.toString))
            .withQueryParam("dateTo", new EqualToPattern(now.toString))
            .withHeader("Authorization", equalTo("Bearer auth-token"))
            .withHeader("Environment", equalTo("test-environment"))
            .willReturn(ok(responseJson))
        )

        running(app) {
          val connector = app.injector.instanceOf[FinancialDataConnector]
          val result = connector.getFinancialData(iossNumber, queryParameters).futureValue

          result mustEqual expectedResult
        }
      }
    }

    "must return None when server returns Not Found" in {
      server.stubFor(
        get(urlEqualTo(s"$financialDataUrl?dateFrom=$dateFrom&dateTo=${queryParameters.toDate.value}"))
          .withQueryParam("dateFrom", new EqualToPattern(dateFrom.toString))
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
        val result = connector.getFinancialData(iossNumber, queryParameters).futureValue
        result mustBe None
      }

    }

    "must return None when server returns an error" in {
      server.stubFor(
        get(urlEqualTo(s"$financialDataUrl?dateFrom=$dateFrom&dateTo=${queryParameters.toDate.value}"))
          .withQueryParam("dateFrom", new EqualToPattern(dateFrom.toString))
          .withQueryParam("dateTo", new EqualToPattern(now.toString))
          .withHeader("Authorization", equalTo("Bearer auth-token"))
          .withHeader("Environment", equalTo("test-environment"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
          )
      )

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]
        val result = connector.getFinancialData(iossNumber, queryParameters).futureValue
        result mustBe None
      }

    }

    "must return None" - {
      "when server returns Http Exception" in {
        server.stubFor(
          get(urlEqualTo(s"$financialDataUrl?dateFrom=$dateFrom&dateTo=${queryParameters.toDate.value}"))
            .withQueryParam("dateFrom", new EqualToPattern(dateFrom.toString))
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
          whenReady(connector.getFinancialData(iossNumber, queryParameters), Timeout(Span(30, Seconds))) { exp =>
            exp mustBe None
          }

        }
      }

      Seq(BAD_REQUEST, SERVICE_UNAVAILABLE, IM_A_TEAPOT).foreach {
        status =>
          s"when server returns status $status" in {
            server.stubFor(
              get(urlEqualTo(s"$financialDataUrl?dateFrom=$dateFrom&dateTo=${queryParameters.toDate.value}"))
                .withQueryParam("dateFrom", new EqualToPattern(dateFrom.toString))
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
              val result = connector.getFinancialData(iossNumber, queryParameters).futureValue
              result mustBe None
            }
          }
      }

    }
  }

}

trait FinancialDataConnectorFixture {
  self: SpecBase =>

  val zonedNow: ZonedDateTime = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

  val dateFrom: LocalDate = zonedNow.toLocalDate.minusMonths(1)
  val dateTo: LocalDate = zonedNow.toLocalDate
  val queryParameters: FinancialDataQueryParameters = FinancialDataQueryParameters(fromDate = Some(dateFrom), toDate = Some(dateTo))

  val responseJson: String =
    s"""{
       | "idType": "IOSS",
       | "idNumber": "123456789",
       | "regimeType": "ECOM",
       | "processingDate": "${zonedNow.toString}",
       | "financialTransactions": [
       |   {
       |     "chargeType": "G Ret AT EU-OMS",
       |     "taxPeriodFrom": "$dateFrom",
       |     "taxPeriodTo": "$dateTo",
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

  val financialTransactions: Seq[FinancialTransaction] = Seq(
    FinancialTransaction(
      chargeType = Some("G Ret AT EU-OMS"),
      mainType = None,
      taxPeriodFrom = Some(dateFrom),
      taxPeriodTo = Some(dateTo),
      originalAmount = Some(BigDecimal(1000)),
      outstandingAmount = Some(BigDecimal(500)),
      clearedAmount = Some(BigDecimal(500)),
      items = Some(items)
    )
  )

  val expectedResult: FinancialDataResponse = Some(FinancialData(
    idType = Some("IOSS"),
    idNumber = Some("123456789"),
    regimeType = Some("ECOM"),
    processingDate = zonedNow,
    financialTransactions = Option(financialTransactions)))
}