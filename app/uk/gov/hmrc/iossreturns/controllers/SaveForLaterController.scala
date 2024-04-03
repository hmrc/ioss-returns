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
import uk.gov.hmrc.iossreturns.controllers.actions.{AuthAction, AuthenticatedControllerComponents}
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.requests.SaveForLaterRequest
import uk.gov.hmrc.iossreturns.services.SaveForLaterService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SaveForLaterController @Inject()(
                                      cc: AuthenticatedControllerComponents,
                                      saveForLaterService: SaveForLaterService,
                                      auth: AuthAction
                                      )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def post(): Action[SaveForLaterRequest] = auth(parse.json[SaveForLaterRequest]).async {
    implicit request =>
      saveForLaterService.saveAnswers(request.body).map {
         answers => Created(Json.toJson(answers))
      }
  }

  def get(): Action[AnyContent] = auth.async {
    implicit request =>
      saveForLaterService.get(request.vrn).map {
        value => value.sortBy(_.lastUpdated).lastOption
          .map(savedUserAnswers => Ok(Json.toJson(savedUserAnswers)))
          .getOrElse(NotFound)
      }
  }

  def delete(period: Period): Action[AnyContent] = auth.async {
    implicit request =>
      saveForLaterService.delete(request.vrn, period).map(
        result => Ok(Json.toJson(result)))
  }
}
