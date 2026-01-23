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

package controllers

import org.mockito.ArgumentMatchers.{any, anyString, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossreturns.controllers.routes
import uk.gov.hmrc.iossreturns.models.requests.SaveForLaterRequest
import uk.gov.hmrc.iossreturns.models.{Period, SavedUserAnswers}
import uk.gov.hmrc.iossreturns.services.SaveForLaterService
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

class SaveForLaterControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach {

  private val mockSaveForLaterService: SaveForLaterService = mock[SaveForLaterService]
  private val s4lRequest: SaveForLaterRequest = arbitrarySaveForLaterRequest.arbitrary.sample.value
  private val savedAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value
  private val period: Period = arbitraryPeriod.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterService)
  }

  "SaveForLaterController" - {

    ".post" - {

      lazy val request =
        FakeRequest(POST, routes.SaveForLaterController.post().url)
          .withJsonBody(Json.toJson(s4lRequest))

      "must save a VAT return and respond with Created" in {

        when(mockSaveForLaterService.saveAnswers(any())) thenReturn savedAnswers.toFuture

        val app =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(app) {

          val result = route(app, request).value

          status(result) `mustBe` CREATED
          contentAsJson(result) mustBe Json.toJson(savedAnswers)
          verify(mockSaveForLaterService, times(1)).saveAnswers(eqTo(s4lRequest))
        }
      }

      "must respond with Unauthorized when the user is not authorised" in {

        val app =
          new GuiceApplicationBuilder()
            .overrides(bind[AuthConnector].toInstance(new FakeFailingAuthConnector(new MissingBearerToken)))
            .build()

        running(app) {

          val result = route(app, request).value
          status(result) `mustBe` UNAUTHORIZED
        }
      }
    }

    ".get" - {

      lazy val request =
        FakeRequest(GET, routes.SaveForLaterController.get(iossNumber).url)

      "must return OK and a response when Saved User Answers are found for the vrn and period" in {

        when(mockSaveForLaterService.get(anyString())) thenReturn Seq(savedAnswers).toFuture

        val app =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(app) {

          val result = route(app, request).value

          status(result) `mustBe` OK
          contentAsJson(result) mustBe Json.toJson(savedAnswers)
          verify(mockSaveForLaterService, times(1)).get(anyString())
        }
      }

      "must return NOT_FOUND when no answers are found" in {

        when(mockSaveForLaterService.get(anyString())) thenReturn Seq().toFuture

        val app =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(app) {

          val result = route(app, request).value

          status(result) `mustBe` NOT_FOUND
          verify(mockSaveForLaterService, times(1)).get(anyString())
        }
      }
    }

    ".delete" - {

      lazy val request =
        FakeRequest(GET, routes.SaveForLaterController.delete(iossNumber, period).url)

      "must return OK" in {

        when(mockSaveForLaterService.delete(any(), any())) thenReturn true.toFuture

        val app =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(app) {

          val result = route(app, request).value

          status(result) `mustBe` OK
          contentAsJson(result) mustBe Json.toJson(true)
          verify(mockSaveForLaterService, times(1)).delete(any(), any())
        }
      }
    }

    ".postForIntermediary" - {

      lazy val request =
        FakeRequest(POST, routes.SaveForLaterController.postForIntermediary().url)
          .withJsonBody(Json.toJson(s4lRequest))

      "must save a VAT return and respond with Created" in {

        when(mockSaveForLaterService.saveAnswers(any())) thenReturn savedAnswers.toFuture

        val application =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(application) {

          val result = route(application, request).value

          status(result) `mustBe` CREATED
          contentAsJson(result) `mustBe` Json.toJson(savedAnswers)
          verify(mockSaveForLaterService, times(1)).saveAnswers(eqTo(s4lRequest))
        }
      }

      "must respond with Unauthorized when the user is not authorised" in {

        val application =
          new GuiceApplicationBuilder()
            .overrides(bind[AuthConnector].toInstance(new FakeFailingAuthConnector(new MissingBearerToken)))
            .build()

        running(application) {

          val result = route(application, request).value
          status(result) `mustBe` UNAUTHORIZED
        }
      }
    }

    ".getForIntermediary" - {

      val savedAnswers: Seq[SavedUserAnswers] = Gen.listOfN(3, arbitrarySavedUserAnswers.arbitrary.sample.value).sample.value

      lazy val request = FakeRequest(GET, routes.SaveForLaterController.getForIntermediary().url)

      "must return OK and a response when Saved User Answers are found for the intermediaries clients" in {

        when(mockSaveForLaterService.get(any[Seq[String]]())) thenReturn savedAnswers.toFuture

        val application =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(application) {

          val result = route(application, request).value

          status(result) `mustBe` OK
          contentAsJson(result) `mustBe` Json.toJson(savedAnswers)
          verify(mockSaveForLaterService, times(1)).get(any[Seq[String]]())
        }
      }

      "must return Seq.empty when no answers are found" in {

        when(mockSaveForLaterService.get(any[Seq[String]]())) thenReturn Seq().toFuture

        val application =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(application) {

          val result = route(application, request).value

          status(result) `mustBe` OK
          contentAsJson(result) `mustBe` Json.arr()
          verify(mockSaveForLaterService, times(1)).get(any[Seq[String]]())
        }
      }
    }

    ".deleteForIntermediary" - {

      val iossNumber: String = arbitrary[String].sample.value

      lazy val request =
        FakeRequest(GET, routes.SaveForLaterController.deleteForIntermediary(iossNumber, period).url)

      "must return OK" in {

        when(mockSaveForLaterService.delete(any(), any())) thenReturn true.toFuture

        val app =
          applicationBuilder()
            .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
            .build()

        running(app) {

          val result = route(app, request).value

          status(result) `mustBe` OK
          contentAsJson(result) mustBe Json.toJson(true)
          verify(mockSaveForLaterService, times(1)).delete(any(), any())
        }
      }
    }
  }
}
