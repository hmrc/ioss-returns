package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, EisErrorResponse, EtmpDisplayReturnError, GatewayTimeout, Period, ServerError}
import uk.gov.hmrc.iossreturns.models.etmp.EtmpVatReturn

import java.time.{Instant, Month}
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
        "microservice.services.etmp-display-vat-return.environment" -> "test-environment"
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

  "get" - {
    val iossNumber = "IM9001234567"
    val period = Period(2023, Month.NOVEMBER)
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
}
