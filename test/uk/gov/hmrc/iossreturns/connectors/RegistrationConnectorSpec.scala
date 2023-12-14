package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.EtmpRegistration

class RegistrationConnectorSpec extends SpecBase with WireMockHelper {

  private val etmpRegistration: EtmpRegistration = arbitraryEtmpRegistration.arbitrary.sample.value

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.ioss-registration.port" -> server.port)
      .build()

  ".getRegistration" - {

    val url = "/ioss-registration/registration"

    "must return an ETMP registration when the backend successfully returns one" in {

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val responseBody = Json.toJson(etmpRegistration).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getRegistration().futureValue

        result mustBe etmpRegistration
      }
    }
  }
}