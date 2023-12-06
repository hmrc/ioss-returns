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
import play.api.Logging
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.iossreturns.models.{ErrorResponse, EtmpDisplayReturnError, ServerError}
import uk.gov.hmrc.iossreturns.models.etmp.EtmpVatReturn

object EtmpDisplayVatReturnHttpParser extends Logging {

  type EtmpDisplayVatReturnResponse = Either[ErrorResponse, EtmpVatReturn]

  implicit object EtmpVatReturnReads extends HttpReads[EtmpDisplayVatReturnResponse] {
    override def read(method: String, url: String, response: HttpResponse): EtmpDisplayVatReturnResponse =
      response.status match {
        case OK =>
          response.json.validate[EtmpVatReturn] match {
            case JsSuccess(vatReturn, _) =>
              Right(vatReturn)
            case JsError(errors) =>
              logger.error(s"Error parsing JSON response from ETMP $errors")
              Left(ServerError)
          }
        case status =>
          logger.info(s"Response received from etmp display vat return ${response.status} with body ${response.body}")
          if(response.body.isEmpty) {
            Left(
              EtmpDisplayReturnError(s"UNEXPECTED_$status", "The response body was empty")
            )
          } else {
            logger.error(s"Unexpected error response from core $url, received status $status, body of response was: ${response.body}")
            Left(
              EtmpDisplayReturnError(status.toString, response.body)
            )
          }
      }

  }

}
