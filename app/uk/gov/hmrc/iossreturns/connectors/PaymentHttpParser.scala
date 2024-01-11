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

import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.iossreturns.models.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import uk.gov.hmrc.iossreturns.models.payments.PaymentResponse

object PaymentHttpParser extends Logging {

  type ReturnPaymentResponse = Either[ErrorResponse, PaymentResponse]

  implicit object ReturnPaymentReads extends HttpReads[ReturnPaymentResponse] {

    override def read(method: String, url: String, response: HttpResponse): ReturnPaymentResponse = {
      response.status match {
        case CREATED =>
          response.json.validate[PaymentResponse] match {
            case JsSuccess(r, _) => Right(r)
            case JsError(errors) =>
              logger.error(s"PaymentResponse: ${response.json}, failed to parse with errors: $errors.")
              Left(InvalidJson)
          }
        case status =>
          logger.error(s"PaymentResponse received unexpected error with status: ${response.status}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }
}