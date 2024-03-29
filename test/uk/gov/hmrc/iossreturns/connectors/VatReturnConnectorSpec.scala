package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.http.Status.{GATEWAY_TIMEOUT, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models._
import uk.gov.hmrc.iossreturns.models.etmp.{EtmpObligations, EtmpObligationsQueryParameters, EtmpVatReturn}
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter

import java.time.{Instant, LocalDate, Month}
import java.util.UUID

class VatReturnConnectorSpec extends SpecBase with WireMockHelper {

  private def application: Application =
    applicationBuilder()
      .configure(
        "microservice.services.core-vat-return.host" -> "127.0.0.1",
        "microservice.services.core-vat-return.port" -> server.port,
        "microservice.services.core-vat-return.authorizationToken" -> "auth-token",
        "microservice.services.core-vat-return.environment" -> "test-environment",
        "microservice.services.etmp-display-vat-return.host" -> "127.0.0.1",
        "microservice.services.etmp-display-vat-return.port" -> server.port,
        "microservice.services.etmp-display-vat-return.authorizationToken" -> "auth-token",
        "microservice.services.etmp-display-vat-return.environment" -> "test-environment",
        "microservice.services.etmp-list-obligations.host" -> "127.0.0.1",
        "microservice.services.etmp-list-obligations.port" -> server.port,
        "microservice.services.etmp-list-obligations.authorizationToken" -> "auth-token",
        "microservice.services.etmp-list-obligations.environment" -> "test-environment",
        "microservice.services.etmp-list-obligations.idType" -> "IOSS",
        "microservice.services.etmp-list-obligations.regimeType" -> "IOSS"
      )
      .build()

  "submit" - {
    val url = "/ioss-returns-stub/vec/submitvatreturn/v1/ioss"
    "when the server returns ACCEPTED" - {
      "must return gracefully" in {
        val app = application

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse().withStatus(202))
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.submit(coreVatReturn).futureValue

          result mustBe Right(())
        }
      }
    }

    "when the server returns an error" - {
      "the error is parseable" in {

        val app = application

        val timestamp = "2021-01-18T12:40:45Z"
        val uuid = "f3204b9d-ed02-4d6f-8ff6-2339daef8241"

        val errorResponseJson =
          s"""{"errorDetail": {
             |  "timestamp": "$timestamp",
             |  "transactionId": "$uuid",
             |  "errorCode": "OSS_405",
             |  "errorMessage": "Method Not Allowed"
             |}}""".stripMargin

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(404)
              .withBody(errorResponseJson)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.submit(coreVatReturn).futureValue

          val expectedResponse = EisErrorResponse(CoreErrorResponse(Instant.parse(timestamp), Some(UUID.fromString(uuid)), "OSS_405", "Method Not Allowed"))

          result mustBe Left(expectedResponse)
        }

      }

      "Http Exception must result in EisErrorResponse" in {

        val app = application

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(504)
              .withFixedDelay(21000)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          whenReady(connector.submit(coreVatReturn), Timeout(Span(30, Seconds))) { exp =>
            exp.isLeft mustBe true
            exp.left.toOption.get mustBe a[EisErrorResponse]
          }

        }

      }

      "the error is not parseable" in {

        val app = application

        val errorResponseJson = """{}"""

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(404)
              .withBody(errorResponseJson)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.submit(coreVatReturn).futureValue

          val expectedResponse = EisErrorResponse(CoreErrorResponse(result.left.toOption.get.errorDetail.timestamp, result.left.toOption.get.errorDetail.transactionId, s"UNEXPECTED_404", errorResponseJson))

          result mustBe Left(expectedResponse)
        }
      }

      "the response has no json body" in {

        val app = application


        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(404)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.submit(coreVatReturn).futureValue

          val expectedResponse = EisErrorResponse(CoreErrorResponse(result.left.toOption.get.errorDetail.timestamp, result.left.toOption.get.errorDetail.transactionId, "UNEXPECTED_404", "The response body was empty"))

          result mustBe Left(expectedResponse)
        }
      }
    }

  }

  "getRegistration" - {
    val iossNumber = "IM9001234567"
    val period = StandardPeriod(2023, Month.NOVEMBER)
    val etmpPeriodKey = "23AK"
    val url = s"/ioss-returns-stub/vec/iossreturns/viewreturns/v1/$iossNumber/$etmpPeriodKey"
    "when the server returns OK" - {
      "and json is parsable" - {
        "must return gracefully" in {
          val etmpVatReturn = arbitrary[EtmpVatReturn].sample.value
          val jsonStringBody = Json.toJson(etmpVatReturn).toString()

          val app = application

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(200)
                  .withBody(jsonStringBody)
              )
          )

          running(app) {
            val connector = app.injector.instanceOf[VatReturnConnector]
            val result = connector.get(iossNumber, period).futureValue

            result mustBe Right(etmpVatReturn)
          }
        }
      }

      "and json is invalid" - {
        "must return an error" in {
          val app = application
          val invalidJson = """{"notvalid":"json"}"""

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(200)
                  .withBody(invalidJson)
              )
          )

          running(app) {
            val connector = app.injector.instanceOf[VatReturnConnector]
            val result = connector.get(iossNumber, period).futureValue

            result mustBe Left(ServerError)
          }
        }
      }

    }

    "when the server returns an error" - {

      "Http Exception must result in GatewayTimeout" in {

        val app = application

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(504)
              .withFixedDelay(21000)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          whenReady(connector.get(iossNumber, period), Timeout(Span(30, Seconds))) { exp =>
            exp.isLeft mustBe true
            exp.left.toOption.get mustBe GatewayTimeout
          }

        }

      }

      "it's handled and returned" in {

        val app = application

        val errorResponseJson = """{}"""

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(404)
              .withBody(errorResponseJson)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.get(iossNumber, period).futureValue

          val expectedResponse = EtmpDisplayReturnError("404", errorResponseJson)

          result mustBe Left(expectedResponse)
        }
      }

      "the response has no json body" in {

        val app = application


        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(404)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.get(iossNumber, period).futureValue

          val expectedResponse = EtmpDisplayReturnError("UNEXPECTED_404", "The response body was empty")

          result mustBe Left(expectedResponse)
        }
      }
    }
  }

  "getObligations" - {

    val idType = "IOSS"
    val iossNumber = "IM9001234567"
    val regimeType = "IOSS"
    val dateFrom = LocalDate.now(stubClockAtArbitraryDate).format(etmpDateFormatter)
    val dateTo = LocalDate.now(stubClockAtArbitraryDate).format(etmpDateFormatter)

    val queryParameters: EtmpObligationsQueryParameters = EtmpObligationsQueryParameters(fromDate = dateFrom, toDate = dateTo, status = None)
    val obligationsUrl = s"/ioss-returns-stub/enterprise/obligation-data/$idType/$iossNumber/$regimeType"

    "must return OK when server return OK and a recognised payload without a status" in {

      val obligations = arbitrary[EtmpObligations].sample.value
      val jsonStringBody = Json.toJson(obligations).toString()

      val app = application

      server.stubFor(
        get(urlEqualTo(s"$obligationsUrl?from=${queryParameters.fromDate}&to=${queryParameters.toDate}"))
          .withQueryParam("from", new EqualToPattern(dateFrom))
          .withQueryParam("to", new EqualToPattern(dateTo))
          .withHeader("Authorization", equalTo("Bearer auth-token"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(jsonStringBody)
          )
      )

      running(app) {
        val connector = app.injector.instanceOf[VatReturnConnector]
        val result = connector.getObligations(iossNumber, queryParameters).futureValue

        result mustBe Right(obligations)
      }
    }
    "must return OK when server return OK and a recognised payload with a status" in {

      val status = "F"
      val queryParameters: EtmpObligationsQueryParameters = EtmpObligationsQueryParameters(fromDate = dateFrom, toDate = dateTo, status = Some(status))

      val obligations = arbitrary[EtmpObligations].sample.value
      val jsonStringBody = Json.toJson(obligations).toString()

      val app = application

      server.stubFor(
        get(urlEqualTo(s"$obligationsUrl?from=${queryParameters.fromDate}&to=${queryParameters.toDate}&status=$status"))
          .withQueryParam("from", new EqualToPattern(dateFrom))
          .withQueryParam("to", new EqualToPattern(dateTo))
          .withQueryParam("status", new EqualToPattern(status))
          .withHeader("Authorization", equalTo("Bearer auth-token"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(jsonStringBody)
          )
      )

      running(app) {
        val connector = app.injector.instanceOf[VatReturnConnector]
        val result = connector.getObligations(iossNumber, queryParameters).futureValue

        result mustBe Right(obligations)
      }
    }

    "when the server returns an error" - {

      "Http Exception must result in GatewayTimeout" in {

        val app = application

        server.stubFor(
          get(urlEqualTo(s"$obligationsUrl?from=${queryParameters.fromDate}&to=${queryParameters.toDate}"))
            .withQueryParam("from", new EqualToPattern(dateFrom))
            .withQueryParam("to", new EqualToPattern(dateTo))
            .withHeader("Authorization", equalTo("Bearer auth-token"))
            .willReturn(aResponse()
              .withStatus(GATEWAY_TIMEOUT)
              .withFixedDelay(21000)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          whenReady(connector.getObligations(iossNumber, queryParameters), Timeout(Span(30, Seconds))) { exp =>
            exp.isLeft mustBe true
            exp.left.toOption.get mustBe GatewayTimeout
          }
        }
      }

      "it's handled and returned" in {

        val app = application

        val errorResponseJson = """{}"""

        server.stubFor(
          get(urlEqualTo(s"$obligationsUrl?from=${queryParameters.fromDate}&to=${queryParameters.toDate}"))
            .withQueryParam("from", new EqualToPattern(dateFrom))
            .withQueryParam("to", new EqualToPattern(dateTo))
            .withHeader("Authorization", equalTo("Bearer auth-token"))
            .willReturn(aResponse()
              .withStatus(NOT_FOUND)
              .withBody(errorResponseJson)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.getObligations(iossNumber, queryParameters).futureValue

          val expectedResponse = EtmpListObligationsError("404", errorResponseJson)

          result mustBe Left(expectedResponse)
        }
      }

      "the response has no json body" in {

        val app = application

        server.stubFor(
          get(urlEqualTo(s"$obligationsUrl?from=${queryParameters.fromDate}&to=${queryParameters.toDate}"))
            .withQueryParam("from", new EqualToPattern(dateFrom))
            .withQueryParam("to", new EqualToPattern(dateTo))
            .withHeader("Authorization", equalTo("Bearer auth-token"))
            .willReturn(aResponse()
              .withStatus(NOT_FOUND)
            )
        )

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]
          val result = connector.getObligations(iossNumber, queryParameters).futureValue

          val expectedResponse = EtmpListObligationsError("UNEXPECTED_404", "The response body was empty")

          result mustBe Left(expectedResponse)
        }
      }
    }
  }
}
