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
import org.mockito.Mockito.{times, verify, when}
import org.mockito.Mockito
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
import uk.gov.hmrc.iossreturns.connectors.CoreVatReturnConnector
import uk.gov.hmrc.iossreturns.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, CoreVatReturn, EisErrorResponse}
import uk.gov.hmrc.iossreturns.models.CoreErrorResponse.REGISTRATION_NOT_FOUND

import java.time.Instant
import scala.concurrent.Future

class ReturnControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach {

  private val mockCoreVatReturnConnector = mock[CoreVatReturnConnector]

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
          .overrides(bind[CoreVatReturnConnector].toInstance(mockCoreVatReturnConnector))
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
          .overrides(bind[CoreVatReturnConnector].toInstance(mockCoreVatReturnConnector))
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
          .overrides(bind[CoreVatReturnConnector].toInstance(mockCoreVatReturnConnector))
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

}
