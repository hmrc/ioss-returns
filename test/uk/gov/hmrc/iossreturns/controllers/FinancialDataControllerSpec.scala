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
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataException}
import uk.gov.hmrc.iossreturns.models.payments.{Charge, Payment, PaymentStatus, PrepareData}
import uk.gov.hmrc.iossreturns.services.{FinancialDataService, PaymentsService}
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class FinancialDataControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach
    with OptionValues
    with FinancialDataControllerFixture {

  private val mockFinancialDataService = mock[FinancialDataService]
  private val paymentsService = mock[PaymentsService]


  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataService)
    Mockito.reset(paymentsService)
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

        val result = route(app, FakeRequest(GET, routes.FinancialDataController.get(LocalDate.now(stubClockAtArbitraryDate)).url)).value

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

  ".prepareFinancialData" - {

    lazy val request =
      FakeRequest(GET, routes.FinancialDataController.prepareFinancialData().url)

    "must return paymentData Json when there are due payments and overdue payments" in {
      val now = LocalDate.now(stubClockAtArbitraryDate)
      val periodOverdue1 = Period(now.minusYears(1).getYear, Month.JANUARY)
      val periodOverdue2 = Period(now.minusYears(1).getYear, Month.FEBRUARY)
      val periodDue1 = Period(now.getYear, now.getMonth.plus(1))
      val periodDue2 = Period(now.getYear, now.getMonth.plus(2))
      val paymentOverdue1 = Payment(periodOverdue1, 10, periodOverdue1.paymentDeadline, PaymentStatus.Unpaid)
      val paymentOverdue2 = Payment(periodOverdue2, 10, periodOverdue2.paymentDeadline, PaymentStatus.Unpaid)
      val paymentDue1 = Payment(periodDue1, 10, periodDue1.paymentDeadline, PaymentStatus.Unpaid)
      val paymentDue2 = Payment(periodDue2, 10, periodDue2.paymentDeadline, PaymentStatus.Unpaid)

      when(paymentsService.getUnpaidPayments(any(), any())(any(), any())) thenReturn Future.successful(List(paymentDue1, paymentDue2, paymentOverdue1, paymentOverdue2))

      val app = applicationBuilder().overrides(bind[PaymentsService].to(paymentsService))
        .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(
          PrepareData(List(paymentDue1, paymentDue2),
            List(paymentOverdue1, paymentOverdue2),
            List(paymentDue1,
              paymentDue2,
              paymentOverdue1,
              paymentOverdue2
            ).map(_.amountOwed).sum,
            List(paymentOverdue1, paymentOverdue2).map(_.amountOwed).sum,
            iossNumber)
        )


      }
    }
  }

  ".getCharge" - {

    lazy val request = FakeRequest(GET, routes.FinancialDataController.getCharge(period).url)

    "must return charge data when service responds with valid charge data" in {

      val charge: Charge = arbitraryCharge.arbitrary.sample.value

      val application = applicationBuilder()
        .overrides(bind[FinancialDataService].toInstance(mockFinancialDataService))
        .build()

      when(mockFinancialDataService.getCharge(any(), any())) thenReturn Some(charge).toFuture

      running(application) {

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(charge)
      }
    }

    "must throw an Exception if the API call fails" in {

      val application = applicationBuilder()
        .overrides(bind[FinancialDataService].toInstance(mockFinancialDataService))
        .build()

      when(mockFinancialDataService.getCharge(any(), any())) thenReturn
        Future.failed(FinancialDataException("Some exception"))

      running(application) {

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must respond with Unauthorized when the user is not authorised" in {

      val application = new GuiceApplicationBuilder()
        .overrides(bind[AuthConnector].toInstance(new FakeFailingAuthConnector(new MissingBearerToken)))
        .build()

      running(application) {

        val result = route(application, request).value

        status(result) mustBe UNAUTHORIZED
      }
    }
  }
}

trait FinancialDataControllerFixture {
  self: SpecBase =>

  val financialData: FinancialData = arbitraryFinancialData.arbitrary.sample.value
  val commencementDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
}