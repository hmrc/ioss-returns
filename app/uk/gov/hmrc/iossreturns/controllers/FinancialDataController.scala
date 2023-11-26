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
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.financialdata.FinancialData._
import uk.gov.hmrc.iossreturns.services.FinancialDataService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FinancialDataController @Inject()(
                                         cc: DefaultAuthenticatedControllerComponents,
                                         service: FinancialDataService,
                                         clock: Clock
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) {
  def get(commencementDate: LocalDate): Action[AnyContent] = cc.auth().async {
    implicit request => {
      service.getFinancialData(request.iossNumber, Some(commencementDate), Some(LocalDate.now(clock)))
        .map(data =>
          Ok(Json.toJson(data))
        )
    }
  }
}
