package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.Period.toEtmpPeriodString
import uk.gov.hmrc.iossreturns.models.corrections.ReturnCorrectionValue
import uk.gov.hmrc.iossreturns.models.{Country, InvalidJson, UnexpectedResponseStatus}

class ReturnCorrectionConnectorSpec
  extends SpecBase
    with WireMockHelper {

  private val returnCorrectionValueResponse: ReturnCorrectionValue = arbitraryReturnCorrectionValue.arbitrary.sample.value

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val periodKey = toEtmpPeriodString(arbitraryPeriod.arbitrary.sample.value)

  private def application: Application =
    applicationBuilder()
      .configure(
        "microservice.services.return-correction.port" -> server.port(),
        "microservice.services.return-correction.host" -> "127.0.0.1",
        "microservice.services.return-correction.authorizationToken" -> "auth-token",
        "microservice.services.return-correction.environment" -> "test-environment"
      )
      .build()

  "ReturnCorrectionConnector" - {

    val url: String = s"/ioss-returns-stub/vec/iossreturns/returncorrection/v1/$iossNumber/${country.code}/$periodKey"

    "must return Right(ReturnCorrectionValue) when server returns CREATED" in {

      running(application) {

        val connector = application.injector.instanceOf[ReturnCorrectionConnector]

        val responseBody = Json.toJson(returnCorrectionValueResponse).toString()

        server.stubFor(
          get(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseBody)
          )
        )

        val result = connector.getMaximumCorrectionValue(iossNumber, country.code, periodKey).futureValue

        result mustBe Right(returnCorrectionValueResponse)
      }
    }

    "must return Left(InvalidJson) when the response cannot be parsed correctly" in {

      running(application) {

        val connector = application.injector.instanceOf[ReturnCorrectionConnector]

        val responseBody = """{"foo" : "bar"}"""

        server.stubFor(
          get(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseBody)
          )
        )

        val result = connector.getMaximumCorrectionValue(iossNumber, country.code, periodKey).futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when server returns an error" in {

      val errorMessage: String = s"Unexpected response from Return Correction. Received status: $INTERNAL_SERVER_ERROR with response body: "

      running(application) {

        val connector = application.injector.instanceOf[ReturnCorrectionConnector]

        server.stubFor(
          get(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
        )

        val result = connector.getMaximumCorrectionValue(iossNumber, country.code, periodKey).futureValue

        result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, errorMessage))
      }
    }
  }
}
