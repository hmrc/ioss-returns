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

package uk.gov.hmrc.iossreturns.connectors

import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models.financialdata.FinancialData

object FinancialDataHttpParser extends Logging {

  type FinancialDataResponse = Option[FinancialData]

  implicit object FinancialDataReads extends HttpReads[FinancialDataResponse] {
    override def read(method: String, url: String, response: HttpResponse): FinancialDataResponse =
      response.status match {
        case OK =>
          response.json.asOpt[FinancialData]
        case status =>
          logger.error(s"Unexpected response from Financial Data $url, received status $status, body of response was: ${response.body}")
          None
      }
  }

}
