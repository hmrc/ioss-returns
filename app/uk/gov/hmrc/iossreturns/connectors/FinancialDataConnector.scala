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

package uk.gov.hmrc.iossreturns.connectors

import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException}
import uk.gov.hmrc.iossreturns.config.FinancialDataConfig
import uk.gov.hmrc.iossreturns.connectors.FinancialDataHttpParser._
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialDataQueryParameters, UnexpectedResponseStatus}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject()(
                                        http: HttpClient,
                                        financialDataConfig: FinancialDataConfig
                                      )(implicit ec: ExecutionContext) extends Logging {

  private implicit val emptyHc: HeaderCarrier = HeaderCarrier()
  private val headers: Seq[(String, String)] = financialDataConfig.financialDataHeaders

  private def financialDataUrl(iossNumber: String) =
    s"${financialDataConfig.baseUrl}enterprise/financial-data/IOSS/$iossNumber/${financialDataConfig.regimeType}"

  def getFinancialData(iossNumber: String, queryParameters: FinancialDataQueryParameters): Future[FinancialDataResponse] = {
    val url = financialDataUrl(iossNumber)
    http.GET[FinancialDataResponse](
      url,
      queryParameters.toSeqQueryParams,
      headers = headers
    ).recover {
      case e: HttpException =>
        logger.error(s"Unexpected error response getting financial data from $url, received status ${e.responseCode}, body of response was: ${e.message}")
        Left(
          UnexpectedResponseStatus(e.responseCode, s"Unexpected error response getting financial data from $url, received status ${e.responseCode}")
        )
    }
  }
}