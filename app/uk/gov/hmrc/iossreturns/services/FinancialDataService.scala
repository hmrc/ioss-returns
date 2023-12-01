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

import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossreturns.connectors.FinancialDataConnector
import uk.gov.hmrc.iossreturns.connectors.FinancialDataHttpParser.FinancialDataResponse
import uk.gov.hmrc.iossreturns.models.des.DesException
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataQueryParameters}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject()(financialDataConnector: FinancialDataConnector)(implicit executionContext: ExecutionContext) {
  def getFinancialData(vrn: Vrn, fromDate: Option[LocalDate], toDate: Option[LocalDate]): Future[Option[FinancialData]] = {
    val result: Future[FinancialDataResponse] = financialDataConnector.getFinancialData(vrn, FinancialDataQueryParameters(fromDate = fromDate, toDate = toDate))

    result.flatMap {
      case Left(errorResponse) => Future.failed(DesException(errorResponse.body))
      case Right(r) => Future.successful(r)
    }
  }
}