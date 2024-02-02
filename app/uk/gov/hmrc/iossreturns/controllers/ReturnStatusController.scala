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
import play.api.mvc.Results.Ok
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.models.youraccount.{CurrentReturns, Return}
import uk.gov.hmrc.iossreturns.models.youraccount.SubmissionStatus.{Complete, Excluded}
import uk.gov.hmrc.iossreturns.services.ReturnsService

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReturnStatusController @Inject()(
                                        cc: DefaultAuthenticatedControllerComponents,
                                        returnsService: ReturnsService
                                      )(implicit ec: ExecutionContext) {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

  def getCurrentReturns(iossNumber: String): Action[AnyContent] = cc.auth().async {
    implicit request =>
      for {
        availablePeriodsWithStatus <- returnsService.getStatuses(
          iossNumber,
          LocalDate.parse(request.registration.schemeDetails.commencementDate, dateTimeFormatter),
          request.registration.exclusions.toList
        )
      } yield {

        println(s"got status $availablePeriodsWithStatus")

        val incompletePeriods = availablePeriodsWithStatus.filterNot(pws => Seq(Complete, Excluded).contains(pws.status))

        val isExcluded = returnsService.getLastExclusionWithoutReversal(request.registration.exclusions.toList).isDefined
        val finalReturnsCompleted = returnsService.hasSubmittedFinalReturn(request.registration.exclusions.toList, availablePeriodsWithStatus)

        val periodInProgress = None // ToDo: No saved answers, so set to None.
        val oldestPeriod = incompletePeriods.sortBy(_.period).headOption
        val returns = incompletePeriods.sortBy(_.period).map(
          periodWithStatus => Return.fromPeriod(
            periodWithStatus.period,
            periodWithStatus.status,
            periodInProgress.contains(periodWithStatus.period),
            oldestPeriod.contains(periodWithStatus)
          )
        )

        Ok(Json.toJson(CurrentReturns(returns, isExcluded, finalReturnsCompleted)))
      }
  }


}
