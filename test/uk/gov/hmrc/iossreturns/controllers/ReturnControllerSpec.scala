/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossreturns.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.VatReturnConnector
import uk.gov.hmrc.iossreturns.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossreturns.models.*
import uk.gov.hmrc.iossreturns.models.CoreErrorResponse.REGISTRATION_NOT_FOUND
import uk.gov.hmrc.iossreturns.models.audit.{CoreVatReturnAuditModel, SubmissionResult}
import uk.gov.hmrc.iossreturns.models.etmp.{EtmpObligations, EtmpVatReturn}
import uk.gov.hmrc.iossreturns.services.{AuditService, SaveForLaterService}
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import java.time.{Instant, Month}
import scala.concurrent.Future

class ReturnControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach {

  private val mockCoreVatReturnConnector = mock[VatReturnConnector]
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockSaveForLaterService: SaveForLaterService = mock[SaveForLaterService]

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockCoreVatReturnConnector,
      mockAuditService,
      mockSaveForLaterService
    )
  }

  ".submit" - {

    val vatReturn = arbitrary[CoreVatReturn].sample.value
    val jsonVatReturn = Json.toJson(vatReturn)
    val readVatReturn = jsonVatReturn.as[CoreVatReturn]

    lazy val request =
      FakeRequest(POST, routes.ReturnController.submit().url)
        .withJsonBody(jsonVatReturn)

    "must save a VAT return, delete any saved return that exists for that client and period and audit a success event and respond with Created" in {

      val period: Period = Period.fromString(readVatReturn.period.toString).value

      when(mockCoreVatReturnConnector.submit(any())) thenReturn Right(()).toFuture

      when(mockSaveForLaterService.delete(any(), any())) thenReturn true.toFuture

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService),
        bind[SaveForLaterService].toInstance(mockSaveForLaterService)
      ).build()

      running(app) {

        val result = route(app, request).value

        val expectedAuditEvent = CoreVatReturnAuditModel(
          "id",
          "",
          vrn.vrn,
          readVatReturn,
          SubmissionResult.Success,
          None
        )

        status(result) mustEqual CREATED
        verify(mockCoreVatReturnConnector, times(1)).submit(eqTo(readVatReturn))
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        verify(mockSaveForLaterService, times(1)).delete(eqTo(iossNumber), eqTo(period))
      }
    }

    "must throw an Exception if there's an error converting the Core period to a Period" in {

      val vatReturn = arbitrary[CoreVatReturn].sample.value
        .copy(period = CorePeriod(arbitrary[Int].sample.value, arbitrary[String].sample.value))
      val jsonVatReturn = Json.toJson(vatReturn)
      val readVatReturn = jsonVatReturn.as[CoreVatReturn]

      lazy val request =
        FakeRequest(POST, routes.ReturnController.submit().url)
          .withJsonBody(jsonVatReturn)

      when(mockCoreVatReturnConnector.submit(any())) thenReturn Right(()).toFuture

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(app) {

        val result = route(app, request).value

        whenReady(result.failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe "There was an error converting Core period to Period."
        }

        verify(mockCoreVatReturnConnector, times(1)).submit(eqTo(readVatReturn))
        verifyNoInteractions(mockAuditService)
        verifyNoInteractions(mockSaveForLaterService)
      }
    }

    "must audit a failure event and respond with NotFound when registration is not in core" in {

      val coreErrorResponse = CoreErrorResponse(Instant.now(stubClockAtArbitraryDate), None, REGISTRATION_NOT_FOUND, "There was an error")
      val eisErrorResponse = EisErrorResponse(coreErrorResponse)

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Left(eisErrorResponse)))

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(app) {

        val result = route(app, request).value

        val expectedAuditEvent = CoreVatReturnAuditModel(
          "id",
          "",
          vrn.vrn,
          readVatReturn,
          SubmissionResult.Failure,
          Some(coreErrorResponse)
        )

        status(result) mustEqual NOT_FOUND
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
      }
    }

    "must audit a failure event and respond with ServiceUnavailable(coreError) when error received from core" in {

      val coreErrorResponse = CoreErrorResponse(Instant.now(stubClockAtArbitraryDate), None, "OSS_111", "There was an error")
      val eisErrorResponse = EisErrorResponse(coreErrorResponse)

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Left(eisErrorResponse)))

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(app) {

        val result = route(app, request).value

        val expectedAuditEvent = CoreVatReturnAuditModel(
          "id",
          "",
          vrn.vrn,
          readVatReturn,
          SubmissionResult.Failure,
          Some(coreErrorResponse)
        )

        status(result) mustEqual SERVICE_UNAVAILABLE
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
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

  ".submitAsIntermediary" - {

    val vatReturn = arbitrary[CoreVatReturn].sample.value
    val jsonVatReturn = Json.toJson(vatReturn)
    val readVatReturn = jsonVatReturn.as[CoreVatReturn]

    lazy val request =
      FakeRequest(POST, routes.ReturnController.submitAsIntermediary(iossNumber).url)
        .withJsonBody(jsonVatReturn)

    "must save a VAT return, delete any saved return that exists for that client and period and audit a success event and respond with Created" in {

      val period: Period = Period.fromString(readVatReturn.period.toString).value

      when(mockCoreVatReturnConnector.submit(any())) thenReturn Right(()).toFuture

      when(mockSaveForLaterService.delete(any(), any())) thenReturn true.toFuture

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService),
        bind[SaveForLaterService].toInstance(mockSaveForLaterService)
      ).build()

      running(app) {

        val result = route(app, request).value

        val expectedAuditEvent = CoreVatReturnAuditModel(
          "id",
          "",
          vrn.vrn,
          readVatReturn,
          SubmissionResult.Success,
          None
        )

        status(result) mustEqual CREATED
        verify(mockCoreVatReturnConnector, times(1)).submit(eqTo(readVatReturn))
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        verify(mockSaveForLaterService, times(1)).delete(eqTo(iossNumber), eqTo(period))
      }
    }

    "must throw an Exception if there's an error converting the Core period to a Period" in {

      val vatReturn = arbitrary[CoreVatReturn].sample.value
        .copy(period = CorePeriod(arbitrary[Int].sample.value, arbitrary[String].sample.value))
      val jsonVatReturn = Json.toJson(vatReturn)
      val readVatReturn = jsonVatReturn.as[CoreVatReturn]

      lazy val request =
        FakeRequest(POST, routes.ReturnController.submitAsIntermediary(iossNumber).url)
          .withJsonBody(jsonVatReturn)

      when(mockCoreVatReturnConnector.submit(any())) thenReturn Right(()).toFuture

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(app) {

        val result = route(app, request).value

        whenReady(result.failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe "There was an error converting Core period to Period."
        }

        verify(mockCoreVatReturnConnector, times(1)).submit(eqTo(readVatReturn))
        verifyNoInteractions(mockAuditService)
        verifyNoInteractions(mockSaveForLaterService)
      }
    }

    "must audit a failure event and respond with NotFound when registration is not in core" in {

      val coreErrorResponse = CoreErrorResponse(Instant.now(stubClockAtArbitraryDate), None, REGISTRATION_NOT_FOUND, "There was an error")
      val eisErrorResponse = EisErrorResponse(coreErrorResponse)

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Left(eisErrorResponse)))

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(app) {

        val result = route(app, request).value

        val expectedAuditEvent = CoreVatReturnAuditModel(
          "id",
          "",
          vrn.vrn,
          readVatReturn,
          SubmissionResult.Failure,
          Some(coreErrorResponse)
        )

        status(result) mustEqual NOT_FOUND
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
      }
    }

    "must audit a failure event and respond with ServiceUnavailable(coreError) when error received from core" in {

      val coreErrorResponse = CoreErrorResponse(Instant.now(stubClockAtArbitraryDate), None, "OSS_111", "There was an error")
      val eisErrorResponse = EisErrorResponse(coreErrorResponse)

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Left(eisErrorResponse)))

      val app = applicationBuilder().overrides(
        bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(app) {

        val result = route(app, request).value

        val expectedAuditEvent = CoreVatReturnAuditModel(
          "id",
          "",
          vrn.vrn,
          readVatReturn,
          SubmissionResult.Failure,
          Some(coreErrorResponse)
        )

        status(result) mustEqual SERVICE_UNAVAILABLE
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
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

  ".get" - {
    val period = StandardPeriod(2023, Month.NOVEMBER)

    lazy val request = FakeRequest(GET, routes.ReturnController.get(period).url)

    "must respond with OK and a sequence of returns when some exist for this user" in {

      val vatReturn = arbitrary[EtmpVatReturn].sample.value

      when(mockCoreVatReturnConnector.get(any(), any())) thenReturn Future.successful(Right(vatReturn))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(vatReturn)
      }
    }

    "must respond with and error when the connector responds with an error" in {

      when(mockCoreVatReturnConnector.get(any(), any())) thenReturn Future.successful(Left(ServerError))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {
        val result = route(app, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
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

  ".getForIossNumber" - {
    val period = StandardPeriod(2023, Month.NOVEMBER)

    lazy val request = FakeRequest(GET, routes.ReturnController.getForIossNumber(period, iossNumber).url)

    "must respond with OK and a sequence of returns when some exist for this user" in {

      val vatReturn = arbitrary[EtmpVatReturn].sample.value

      when(mockCoreVatReturnConnector.get(any(), any())) thenReturn Future.successful(Right(vatReturn))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(vatReturn)
      }
    }

    "must respond with and error when the connector responds with an error" in {

      when(mockCoreVatReturnConnector.get(any(), any())) thenReturn Future.successful(Left(ServerError))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {
        val result = route(app, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
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

  ".getObligations" - {

    val etmpObligations: EtmpObligations = arbitraryObligations.arbitrary.sample.value
    val iossNumber: String = arbitrary[String].sample.value

    lazy val request = FakeRequest(GET, routes.ReturnController.getObligations(iossNumber).url)

    "must respond with OK and return a valid response" in {

      when(mockCoreVatReturnConnector.getObligations(any(), any())) thenReturn Right(etmpObligations).toFuture

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(etmpObligations)
      }
    }

    "must respond with and error when the connector responds with an error" in {

      when(mockCoreVatReturnConnector.getObligations(any(), any())) thenReturn Left(ServerError).toFuture

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {
        val result = route(app, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
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
