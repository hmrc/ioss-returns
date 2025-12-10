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
import uk.gov.hmrc.iossreturns.connectors.VatReturnConnector
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.audit.{CoreVatReturnAuditModel, SubmissionResult}
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligationsQueryParameters
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, CoreVatReturn, Period}
import uk.gov.hmrc.iossreturns.services.AuditService
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnController @Inject()(
                                  cc: DefaultAuthenticatedControllerComponents,
                                  coreVatReturnConnector: VatReturnConnector,
                                  auditService: AuditService,
                                  clock: Clock
                                )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit(): Action[CoreVatReturn] = cc.auth()(parse.json[CoreVatReturn]).async {
    implicit request =>
      coreVatReturnConnector.submit(request.body).map {
        case Right(_) =>
          auditService.audit(CoreVatReturnAuditModel.build(request.body, SubmissionResult.Success, None))
          Created
        case Left(errorResponse) if errorResponse.errorDetail.errorCode == CoreErrorResponse.REGISTRATION_NOT_FOUND =>
          auditService.audit(CoreVatReturnAuditModel.build(request.body, SubmissionResult.Failure, Some(errorResponse.errorDetail)))
          NotFound(Json.toJson(errorResponse.errorDetail))
        case Left(errorResponse) =>
          auditService.audit(CoreVatReturnAuditModel.build(request.body, SubmissionResult.Failure, Some(errorResponse.errorDetail)))
          ServiceUnavailable(Json.toJson(errorResponse.errorDetail))
      }
  }
  
  def submitAsIntermediary(iossNumber: String): Action[CoreVatReturn] = cc.auth(Some(iossNumber))(parse.json[CoreVatReturn]).async {
    implicit request =>
      coreVatReturnConnector.submit(request.body).map {
        case Right(_) =>
          auditService.audit(CoreVatReturnAuditModel.build(request.body, SubmissionResult.Success, None))
          Created
        case Left(errorResponse) if errorResponse.errorDetail.errorCode == CoreErrorResponse.REGISTRATION_NOT_FOUND =>
          auditService.audit(CoreVatReturnAuditModel.build(request.body, SubmissionResult.Failure, Some(errorResponse.errorDetail)))
          NotFound(Json.toJson(errorResponse.errorDetail))
        case Left(errorResponse) =>
          auditService.audit(CoreVatReturnAuditModel.build(request.body, SubmissionResult.Failure, Some(errorResponse.errorDetail)))
          ServiceUnavailable(Json.toJson(errorResponse.errorDetail))
      }
  }

  def get(period: Period): Action[AnyContent] = cc.auth().async { implicit request =>
    get(period, request.iossNumber)
  }

  def getForIossNumber(period: Period, iossNumber: String): Action[AnyContent] = cc.checkIossNumber(iossNumber).async {
    get(period, iossNumber)
  }

  private def get(period: Period, iossNumber: String): Future[Result] = {
    coreVatReturnConnector.get(iossNumber, period).map {
      case Right(vatReturn) => Ok(Json.toJson(vatReturn))
      case Left(errorResponse) => InternalServerError(Json.toJson(errorResponse.body))
    }
  }

  def getObligations(iossNumber: String): Action[AnyContent] = cc.checkIossNumber(iossNumber).async {
    implicit request =>

      val fromDate: String = request.registration.schemeDetails.commencementDate.format(etmpDateFormatter)
      val toDate = LocalDate.now(clock).plusMonths(1).withDayOfMonth(1).minusDays(1).format(etmpDateFormatter)

      val queryParameters: EtmpObligationsQueryParameters = EtmpObligationsQueryParameters(
        fromDate = fromDate,
        toDate = toDate,
        None
      )

      coreVatReturnConnector
        .getObligations(idNumber = iossNumber, queryParameters = queryParameters).map {
        case Right(etmpObligations) => Ok(Json.toJson(etmpObligations))
        case Left(errorResponse) => InternalServerError(Json.toJson(errorResponse.body))
      }
  }
}
