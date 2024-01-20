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

import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData}

object FinancialDataHttpParser extends Logging {

  type FinancialDataResponse = Either[ErrorResponse, Option[FinancialData]]

  implicit object FinancialDataReads extends HttpReads[FinancialDataResponse] {
    override def read(method: String, url: String, response: HttpResponse): FinancialDataResponse =
      response.status match {
        case OK =>
          response.json.validateOpt[FinancialData] match {
            case JsSuccess(value, _) => Right(value)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $url with errors $errors")
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          logger.warn(s"Got not found from financial data $url")
          Right(None)
        case UNPROCESSABLE_ENTITY =>
          logger.warn(s"Got Unprocessable Entity from financial data $url")
          Right(None)
        case status =>
          logger.error(s"Unexpected response from Financial Data $url, received status $status, body of response was: ${response.body}")
          Left(UnexpectedResponseStatus(status, s"Unexpected response from Financial Data, received status $status"))
      }
  }

}
