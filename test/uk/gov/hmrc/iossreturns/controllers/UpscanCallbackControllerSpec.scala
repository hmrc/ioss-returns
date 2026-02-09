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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.services.upscan.UpscanCallbackService

import scala.concurrent.{ExecutionContext, Future}

class UpscanCallbackControllerSpec extends SpecBase with ScalaCheckPropertyChecks {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "UpscanCallbackController" - {

    "handle a success callback and call the service" in {

      val mockService = mock[UpscanCallbackService]
      when(mockService.handleUpscanCallback(any())).thenReturn(Future.successful(()))

      val app = applicationBuilder()
        .overrides(bind[UpscanCallbackService].toInstance(mockService))
        .build()

      running(app) {
        val json = Json.parse(
          """
                {
                  "reference": "123",
                  "fileStatus": "READY",
                  "uploadDetails": {
                    "fileName": "test.csv",
                    "fileMimeType": "text/csv",
                    "uploadTimestamp": "2026-02-09T12:00:00Z",
                    "checksum": "abc123",
                    "size": 1024
                  }
                }
              """)

        val request = FakeRequest(POST, routes.UpscanCallbackController.callback.url).withJsonBody(json)
        val result = route(app, request).value

        status(result) mustEqual OK
        verify(mockService).handleUpscanCallback(any())

      }
    }

    "handle a failure callback and call the service" in {

      val mockService = mock[UpscanCallbackService]
      when(mockService.handleUpscanCallback(any())).thenReturn(Future.successful(()))

      val app = applicationBuilder()
        .overrides(bind[UpscanCallbackService].toInstance(mockService))
        .build()

      running(app) {
        val json = Json.parse(
          """
                {
                  "reference": "123",
                  "fileStatus": "FAILED",
                  "failureDetails": {
                  "failureReason": "QUARANTINE"
                  }
                }
              """)

        val request = FakeRequest(POST, routes.UpscanCallbackController.callback.url).withJsonBody(json)
        val result = route(app, request).value

        status(result) mustEqual OK
        verify(mockService).handleUpscanCallback(any())
      }
    }

    "return BadRequest on invalid JSON" in {

      val mockService = mock[UpscanCallbackService]

      val app = applicationBuilder()
        .overrides(bind[UpscanCallbackService].toInstance(mockService))
        .build()

      running(app) {
        val json = Json.parse("""{"foo": "bar"}""")
        val request = FakeRequest(POST, routes.UpscanCallbackController.callback.url).withJsonBody(json)
        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
        verifyNoInteractions(mockService)
      }
    }
  }
}
