package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalacheck.Arbitrary.arbitrary
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.etmp.intermediary.IntermediaryRegistrationWrapper

class IntermediaryRegistrationConnectorSpec extends SpecBase with WireMockHelper {

  private val registrationWrapper: IntermediaryRegistrationWrapper = arbitrary[IntermediaryRegistrationWrapper].sample.value

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.ioss-intermediary-registration.port" -> server.port)
      .build()

  ".get" - {

    val url = s"/ioss-intermediary-registration/get-registration/$iossNumber"

    "must return an ETMP registration when the backend successfully returns one" in {

      running(application) {

        val connector: IntermediaryRegistrationConnector = application.injector.instanceOf[IntermediaryRegistrationConnector]

        val responseBody = Json.toJson(registrationWrapper).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.get(iossNumber).futureValue

        result mustBe registrationWrapper
      }
    }
  }
}
