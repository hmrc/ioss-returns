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
import play.api.mvc.Action
import uk.gov.hmrc.iossreturns.connectors.CoreVatReturnConnector
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, CoreVatReturn}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReturnController @Inject()(
                                  cc: DefaultAuthenticatedControllerComponents,
                                  coreVatReturnConnector: CoreVatReturnConnector
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


}
