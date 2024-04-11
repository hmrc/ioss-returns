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

package uk.gov.hmrc.iossreturns.services

import uk.gov.hmrc.iossreturns.connectors.{FinancialDataConnector, VatReturnConnector}
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligations._
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusion
import uk.gov.hmrc.iossreturns.models.etmp.{EtmpObligations, EtmpObligationsQueryParameters, EtmpVatReturn}
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataQueryParameters}
import uk.gov.hmrc.iossreturns.models.payments.{Payment, PaymentStatus}
import uk.gov.hmrc.iossreturns.models.{EtmpDisplayReturnError, Period}
import uk.gov.hmrc.iossreturns.utils.Formatters._

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsService @Inject()(
                                 financialDataConnector: FinancialDataConnector,
                                 vatReturnConnector: VatReturnConnector,
                                 checkExclusionsService: CheckExclusionsService,
                                 clock: Clock
                               ) {

  def getUnpaidPayments(iossNumber: String, startTime: LocalDate, exclusions: List[EtmpExclusion])(implicit ec: ExecutionContext): Future[List[Payment]] = {
    withFinancialDataAndVatReturns(iossNumber, startTime) {
      (financialDataMaybe, vatReturns) => {
        val vatReturnsForPeriodsWithOutstandingAmounts = filterIfPaymentOutstanding(financialDataMaybe, vatReturns)

        val payments = vatReturnsForPeriodsWithOutstandingAmounts.map(
          vatReturnDue => {
            calculatePayment(
              vatReturnDue, financialDataMaybe, exclusions
            )
          }
        )

        payments
      }
    }
  }

  private def withFinancialDataAndVatReturns[T](iossNumber: String, startTime: LocalDate)
                                               (block: (Option[FinancialData], List[EtmpVatReturn]) => T)(implicit ec: ExecutionContext): Future[T] = {

    val now = LocalDate.now(clock)
    val fromDate: String = startTime.format(etmpDateFormatter)
    val toDate = now.plusMonths(1).withDayOfMonth(1).minusDays(1).format(etmpDateFormatter)

    val queryParameters: EtmpObligationsQueryParameters = EtmpObligationsQueryParameters(
      fromDate = fromDate,
      toDate = toDate,
      status = None
    )

    val financialDataQueryParameters: FinancialDataQueryParameters = FinancialDataQueryParameters(
      fromDate = Some(startTime),
      toDate = Some(now)
    )

    for {
      financialData <- getFinancialData(iossNumber, financialDataQueryParameters)
      obligations <- getObligations(iossNumber, queryParameters)
      vatReturns <- getVatReturnsForObligations(iossNumber, obligations)
    } yield {
      block(financialData, vatReturns)
    }
  }

  def getFinancialData(iossNumber: String, queryParameters: FinancialDataQueryParameters)(implicit ec: ExecutionContext): Future[Option[FinancialData]] = {
    financialDataConnector.getFinancialData(iossNumber, queryParameters).flatMap {
      case Right(obligations) => Future.successful(obligations)
      case Left(e) => Future.failed(new Exception(e.body))
    }
  }

  def getObligations(iossNumber: String, queryParameters: EtmpObligationsQueryParameters)(implicit ec: ExecutionContext): Future[EtmpObligations] = {
    vatReturnConnector.getObligations(iossNumber, queryParameters).flatMap {
      case Right(obligations) => Future.successful(obligations)
      case Left(e) => Future.failed(new Exception(e.body))
    }
  }

  def getVatReturnsForObligations(iossNumber: String, obligations: EtmpObligations)
                                 (implicit ec: ExecutionContext): Future[List[EtmpVatReturn]] =
    Future.sequence(
      obligations.getFulfilledPeriods.map { period =>
        vatReturnConnector.get(iossNumber, period).map {
          case Right(vatReturn) => List(vatReturn)
          case Left(EtmpDisplayReturnError(code, _)) if code startsWith "UNEXPECTED_404" => Nil
        }
      }
    ).map(_.flatten)

  def filterIfPaymentOutstanding(financialDataMaybe: Option[FinancialData], etmpVatReturns: List[EtmpVatReturn]): List[EtmpVatReturn] = {
    etmpVatReturns.filter(vatReturn => {
      val charge = financialDataMaybe.flatMap(_.getChargeForPeriod(Period.fromKey(vatReturn.periodKey)))

      // If some payments "FOR THAT PERIOD" made with outstanding amount
      // or no payments made "FOR THAT PERIOD" but there is vat amount "FOR THAT PERIOD"
      val hasChargeWithOutstanding = charge.exists(_.outstandingAmount > 0)
      val expectingCharge = charge.isEmpty && (vatReturn.totalVATAmountDueForAllMSGBP > 0)

      hasChargeWithOutstanding || expectingCharge
    })
  }

  def calculatePayment(vatReturn: EtmpVatReturn, financialDataMaybe: Option[FinancialData], exclusions: List[EtmpExclusion]): Payment = {
    val period = Period.fromKey(vatReturn.periodKey)
    val charge = for {
      financialData <- financialDataMaybe
      chargeCalculated <- financialData.getChargeForPeriod(period)
    } yield chargeCalculated

    val paymentStatus = if(checkExclusionsService.isPeriodExcluded(period, exclusions)) {
      PaymentStatus.Excluded
    } else {
      charge.getPaymentStatus()
    }

    Payment(period,
      charge.map(c => c.outstandingAmount).getOrElse(vatReturn.totalVATAmountDueForAllMSGBP),
      period.paymentDeadline,
      paymentStatus
    )
  }
}