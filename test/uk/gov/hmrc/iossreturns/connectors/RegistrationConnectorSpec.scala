package uk.gov.hmrc.iossreturns.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.RegistrationWrapper
import uk.gov.hmrc.iossreturns.models.enrolments.EACDEnrolments

class RegistrationConnectorSpec extends SpecBase with WireMockHelper {

  private val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

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

        val responseBody = Json.toJson(registrationWrapper).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getRegistration().futureValue

        result mustBe registrationWrapper
      }
    }
  }

  ".getRegistrationForIossNumber" - {

    val url = s"/ioss-registration/registration/$iossNumber"

    "must return an ETMP registration when the backend successfully returns one" in {

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val responseBody = Json.toJson(registrationWrapper).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getRegistrationForIossNumber(iossNumber).futureValue

        result mustBe registrationWrapper
      }
    }
  }


  ".getAccounts" - {
    val userId = "user-123456"
    val url = s"/ioss-registration/accounts/$userId"

    "must return a registration when the server provides one" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val eACDEnrolments = arbitrary[EACDEnrolments].sample.value

        val responseBody = Json.toJson(eACDEnrolments).toString

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getAccounts(userId).futureValue

        result mustEqual eACDEnrolments
      }
    }

  }
}
