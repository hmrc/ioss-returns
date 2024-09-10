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

import uk.gov.hmrc.iossreturns.connectors.FinancialDataConnector
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataQueryParameters}
import uk.gov.hmrc.iossreturns.models.payments.Charge

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject()(financialDataConnector: FinancialDataConnector, returnsService: ReturnsService)(implicit executionContext: ExecutionContext) {

  def getCharge(iossNumber: String, period: Period): Future[Option[Charge]] = {
    getFinancialDataForDateRange(iossNumber, period.firstDay, period.lastDay).map { maybeFinancialData =>
      maybeFinancialData.flatMap(_.getChargeForPeriod(period))
    }
  }

  def getFinancialData(iossNumber: String, fromDate: Option[LocalDate], toDate: Option[LocalDate]): Future[Option[FinancialData]] = {
    (fromDate, toDate) match {
      case (Some(dateFrom), Some(dateTo)) =>
        val financialDatas: Future[Seq[FinancialData]] =
          Future.sequence(
            returnsService.getPeriodYears(dateFrom, dateTo).map {
              taxYear => getFinancialDataForDateRange(iossNumber, taxYear.startOfYear, taxYear.endOfYear)
            }
          ).map(_.flatten)

        financialDatas.map {
          case firstFinancialData :: Nil => Some(firstFinancialData)
          case firstFinancialData :: rest =>
            val otherFinancialTransactions = rest.flatMap(_.financialTransactions).flatten

            val allTransactions =
              firstFinancialData.financialTransactions.getOrElse(Nil) ++ otherFinancialTransactions

            val maybeAllTransactions = if (allTransactions.isEmpty) None else Some(allTransactions)
            Some(firstFinancialData.copy(financialTransactions = maybeAllTransactions))
          case Nil => None
        }
      case _ =>
        Future.failed(new IllegalArgumentException("Both fromDate and toDate must be provided"))
    }
  }

  private def getFinancialDataForDateRange(iossNumber: String, fromDate: LocalDate, toDate: LocalDate): Future[Option[FinancialData]] = {
    financialDataConnector.getFinancialData(iossNumber, FinancialDataQueryParameters(fromDate = Some(fromDate), toDate = Some(toDate))).flatMap {
        case Some(value) => Future.successful(Some(value))
      case None        => Future.successful(None)
    }
  }
}