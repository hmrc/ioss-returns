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

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.iossreturns.generators.ModelGenerators
import uk.gov.hmrc.iossreturns.models.IOSSNumber
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataException}
import uk.gov.hmrc.iossreturns.services.FinancialDataService

import java.time.LocalDate
import scala.concurrent.Future

class FinancialDataControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach
    with OptionValues {
  
  private val mockFinancialDataService = mock[FinancialDataService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataService)
    super.beforeEach()
  }

  ".getFinancialData" - {

    lazy val request =
      FakeRequest(GET, uk.gov.hmrc.iossreturns.controllers.routes.FinancialDataController.get(FinancialDataControllerFixture.commencementDate, FinancialDataControllerFixture.iossNumber.value).url)

    "error if api errors" in {

      val app =
        applicationBuilder
          .overrides(bind[FinancialDataService].to(mockFinancialDataService))
          .build()

      when(mockFinancialDataService.getFinancialData(IOSSNumber(anyString()), any(), any())) thenReturn Future.failed(FinancialDataException("Some exception"))

      running(app) {

        val result = route(app, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "return financial data returned from downstream" in {
      val app =
        applicationBuilder
          .overrides(bind[FinancialDataService].to(mockFinancialDataService))
          .build()

      when(mockFinancialDataService.getFinancialData(IOSSNumber(anyString()), any(), any())) thenReturn Future.successful(Some(FinancialDataControllerFixture.financialData))

      running(app) {

        val result = route(app, FakeRequest(GET, uk.gov.hmrc.iossreturns.controllers.routes.FinancialDataController.get(LocalDate.now(), FinancialDataControllerFixture.iossNumber.value).url)).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(Some(FinancialDataControllerFixture.financialData))
      }
    }
  }
}

object FinancialDataControllerFixture extends ModelGenerators with OptionValues {
  val iossNumber = arbitraryIOSSNumber.arbitrary.sample.value
  val financialData: FinancialData = arbitraryFinancialData.arbitrary.sample.value
  val commencementDate = LocalDate.now()
}