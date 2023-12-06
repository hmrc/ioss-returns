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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataException}
import uk.gov.hmrc.iossreturns.services.FinancialDataService

import java.time.LocalDate
import scala.concurrent.Future

class FinancialDataControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach
    with OptionValues
    with FinancialDataControllerFixture {

  private val mockFinancialDataService = mock[FinancialDataService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataService)
    super.beforeEach()
  }

  ".getFinancialData" - {

    lazy val request =
      FakeRequest(GET, routes.FinancialDataController.get(commencementDate).url)

    "error if api errors" in {

      val app =
        applicationBuilder()
          .overrides(bind[FinancialDataService].to(mockFinancialDataService))
          .build()

      when(mockFinancialDataService.getFinancialData(any(), any(), any())) thenReturn
        Future.failed(FinancialDataException("Some exception"))

      running(app) {

        val result = route(app, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "return financial data returned from downstream" in {
      val app =
        applicationBuilder()
          .overrides(bind[FinancialDataService].to(mockFinancialDataService))
          .build()

      when(mockFinancialDataService.getFinancialData(any(), any(), any())) thenReturn
        Future.successful(Some(financialData))

      running(app) {

        val result = route(app, FakeRequest(GET, routes.FinancialDataController.get(LocalDate.now()).url)).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(Some(financialData))
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

trait FinancialDataControllerFixture {
  self: SpecBase =>

  val financialData: FinancialData = arbitraryFinancialData.arbitrary.sample.value
  val commencementDate: LocalDate = LocalDate.now()
}