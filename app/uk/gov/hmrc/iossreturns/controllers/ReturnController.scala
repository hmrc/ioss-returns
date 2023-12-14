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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.iossreturns.connectors.VatReturnConnector
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligationsFulfilmentStatus
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, CoreVatReturn, Period}
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReturnController @Inject()(
                                  cc: DefaultAuthenticatedControllerComponents,
                                  coreVatReturnConnector: VatReturnConnector,
                                  clock: Clock
                                )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit(): Action[CoreVatReturn] = cc.auth()(parse.json[CoreVatReturn]).async {
    implicit request =>
      coreVatReturnConnector.submit(request.body).map {
        case Right(_) => Created
        case Left(errorResponse) if (errorResponse.errorDetail.errorCode == CoreErrorResponse.REGISTRATION_NOT_FOUND) => NotFound(Json.toJson(errorResponse.errorDetail))
        case Left(errorResponse) => ServiceUnavailable(Json.toJson(errorResponse.errorDetail))
      }
  }

  def get(period: Period): Action[AnyContent] = cc.auth().async {
    implicit request =>
      coreVatReturnConnector.get(request.iossNumber, period).map {
        case Right(vatReturn) => Ok(Json.toJson(vatReturn))
        case Left(errorResponse) => InternalServerError(Json.toJson(errorResponse.body))
      }
  }

  def getObligations(idNumber: String): Action[AnyContent] = cc.auth().async {
    implicit request =>

      val commencementDate: String = request.registration.schemeDetails.commencementDate.format(etmpDateFormatter)
      val today = LocalDate.now(clock).format(etmpDateFormatter)

      coreVatReturnConnector
        .getObligations(idNumber = idNumber, dateFrom = commencementDate, dateTo = today, status = EtmpObligationsFulfilmentStatus.All.toString).map {
        case Right(etmpObligations) => Ok(Json.toJson(etmpObligations))
        case Left(errorResponse) => InternalServerError(Json.toJson(errorResponse.body))
      }
  }
}
