/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.iossreturns.connectors.ReturnCorrectionConnector
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReturnCorrectionController @Inject()(
                                            cc: DefaultAuthenticatedControllerComponents,
                                            returnCorrectionConnector: ReturnCorrectionConnector
                                          )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getReturnCorrection(iossNumber: String, countryCode: String, period: Period): Action[AnyContent] = cc.auth().async {
    returnCorrectionConnector.getMaximumCorrectionValue(iossNumber, countryCode, period).map {
      case Right(returnCorrection) =>
        Ok(Json.toJson(returnCorrection))
      case Left(errorResponse) =>
        InternalServerError(Json.toJson(errorResponse.body))
    }
  }
}
