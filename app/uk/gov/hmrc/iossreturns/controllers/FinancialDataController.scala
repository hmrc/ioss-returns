/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.financialdata.FinancialData._
import uk.gov.hmrc.iossreturns.models.payments.{PaymentStatus, PrepareData}
import uk.gov.hmrc.iossreturns.services.{FinancialDataService, PaymentsService}
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataController @Inject()(
                                         cc: DefaultAuthenticatedControllerComponents,
                                         financialDataService: FinancialDataService,
                                         paymentsService: PaymentsService,
                                         clock: Clock
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) {
  def get(commencementDate: LocalDate): Action[AnyContent] = cc.auth().async {
    implicit request => {
      financialDataService.getFinancialData(request.iossNumber, Some(commencementDate), Some(LocalDate.now(clock)))
        .map(data =>
          Ok(Json.toJson(data))
        )
    }
  }

  def prepareFinancialData(): Action[AnyContent] = cc.auth().async {
    implicit request => {
      val now = LocalDate.now(clock)
      val startTime = LocalDate.parse(request.registration.schemeDetails.commencementDate, etmpDateFormatter)
      val unpaidPayments = paymentsService.getUnpaidPayments(request.iossNumber, startTime, request.registration.exclusions.toList)
      unpaidPayments.map { up =>
        val totalAmountOwed = up.map(_.amountOwed).sum
        val totalAmountOverdue = up.filter(_.dateDue.isBefore(now)).map(_.amountOwed).sum
        val (overduePayments, duePayments) = up.partition(_.dateDue.isBefore(LocalDate.now(clock)))
        val excludedPayments = overduePayments.filter(_.paymentStatus == PaymentStatus.Excluded)
        val overduePaymentsNotExcluded = overduePayments.filterNot(_.paymentStatus == PaymentStatus.Excluded)
        Ok(Json.toJson(PrepareData(
          duePayments,
          overduePaymentsNotExcluded,
          excludedPayments,
          totalAmountOwed,
          totalAmountOverdue,
          iossNumber = request.iossNumber
        )))
      }
    }
  }

  def getCharge(period: Period): Action[AnyContent] = cc.auth().async { implicit request =>
    getCharge(period, request.iossNumber)
  }

  def getChargeForIossNumber(period: Period, iossNumber: String): Action[AnyContent] = cc.checkIossNumber(iossNumber).async {
    getCharge(period, iossNumber)
  }

  private def getCharge(period: Period, iossNumber: String): Future[Result] = {
    financialDataService.getCharge(iossNumber, period).map { chargeData =>
      Ok(Json.toJson(chargeData))
    }
  }
}
