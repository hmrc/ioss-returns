/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.VatReturnConnector
import uk.gov.hmrc.iossreturns.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossreturns.models.CoreErrorResponse.REGISTRATION_NOT_FOUND
import uk.gov.hmrc.iossreturns.models._
import uk.gov.hmrc.iossreturns.models.etmp.{EtmpObligations, EtmpVatReturn}
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import java.time.{Instant, Month}
import scala.concurrent.Future

class ReturnControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach {

  private val mockCoreVatReturnConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreVatReturnConnector)
  }

  ".submit" - {

    val vatReturn = arbitrary[CoreVatReturn].sample.value

    lazy val request =
      FakeRequest(POST, routes.ReturnController.submit.url)
        .withJsonBody(Json.toJson(vatReturn))

    "must save a VAT return and respond with Created" in {

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Right(())))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual CREATED
        verify(mockCoreVatReturnConnector, times(1)).submit(eqTo(vatReturn))
      }
    }

    "must respond with NotFound when registration is not in core" in {
      val coreErrorResponse = CoreErrorResponse(Instant.now(), None, REGISTRATION_NOT_FOUND, "There was an error")
      val eisErrorResponse = EisErrorResponse(coreErrorResponse)

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Left(eisErrorResponse)))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual NOT_FOUND
      }
    }

    "must respond with ServiceUnavailable(coreError) when error received from core" in {
      val coreErrorResponse = CoreErrorResponse(Instant.now(), None, "OSS_111", "There was an error")
      val eisErrorResponse = EisErrorResponse(coreErrorResponse)

      when(mockCoreVatReturnConnector.submit(any()))
        .thenReturn(Future.successful(Left(eisErrorResponse)))

      val app =
        applicationBuilder()
          .overrides(bind[VatReturnConnector].toInstance(mockCoreVatReturnConnector))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual SERVICE_UNAVAILABLE
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

  ".getRegistration" - {
    val period = Period(2023, Month.NOVEMBER)

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

  ".getObligations" - {

    val etmpObligations: EtmpObligations = arbitraryObligations.arbitrary.sample.value
    val idNumber: String = arbitrary[String].sample.value

    lazy val request = FakeRequest(GET, routes.ReturnController.getObligations(idNumber).url)

    "must respond with OK and return a valid response" in {

      when(mockCoreVatReturnConnector.getObligations(any(), any(), any(), any())) thenReturn Right(etmpObligations).toFuture

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

      when(mockCoreVatReturnConnector.getObligations(any(), any(), any(), any())) thenReturn Left(ServerError).toFuture

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
