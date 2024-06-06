package uk.gov.hmrc.iossreturns.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.ReturnCorrectionConnector
import uk.gov.hmrc.iossreturns.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossreturns.models.corrections.ReturnCorrectionValue
import uk.gov.hmrc.iossreturns.models.{Country, Period, ServerError}
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

class ReturnCorrectionControllerSpec extends SpecBase with BeforeAndAfterEach {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockReturnCorrectionConnector: ReturnCorrectionConnector = mock[ReturnCorrectionConnector]

  private val returnCorrectionValue: ReturnCorrectionValue = arbitraryReturnCorrectionValue.arbitrary.sample.value
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  override val period: Period = arbitraryPeriod.arbitrary.sample.value

  private lazy val request = FakeRequest(GET, routes.ReturnCorrectionController.getReturnCorrection(iossNumber, country.code, period).url)

  override def beforeEach(): Unit =
    Mockito.reset(mockReturnCorrectionConnector)

  "ReturnCorrectionController" - {

    "must return OK and a valid response payload when connector returns Right" in {

      when(mockReturnCorrectionConnector.getMaximumCorrectionValue(any(), any(), any())) thenReturn Right(returnCorrectionValue).toFuture

      val application = applicationBuilder()
        .overrides(bind[ReturnCorrectionConnector].toInstance(mockReturnCorrectionConnector))
        .build()

      running(application) {
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(returnCorrectionValue)
      }
    }

    "must return InternalServerError when connector returns an error" in {

      when(mockReturnCorrectionConnector.getMaximumCorrectionValue(any(), any(), any())) thenReturn Left(ServerError).toFuture

      val application = applicationBuilder()
        .overrides(bind[ReturnCorrectionConnector].toInstance(mockReturnCorrectionConnector))
        .build()

      running(application) {
        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "must respond with Unauthorized when the user is not authorised" in {

      val app =
        new GuiceApplicationBuilder()
          .overrides(bind[AuthConnector].toInstance(new FakeFailingAuthConnector(new MissingBearerToken)))
          .build()

      running(app) {
        val result = route(app, request).value
        status(result) mustEqual UNAUTHORIZED
      }
    }
  }
}
