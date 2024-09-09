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
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.iossreturns.controllers.actions.{AuthorisedRequest, DefaultAuthenticatedControllerComponents}
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.youraccount.SubmissionStatus.{Complete, Excluded, Expired}
import uk.gov.hmrc.iossreturns.models.youraccount.{CurrentReturns, PeriodWithStatus, Return}
import uk.gov.hmrc.iossreturns.repository.SaveForLaterRepository
import uk.gov.hmrc.iossreturns.services.{CheckExclusionsService, ReturnsService}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnStatusController @Inject()(
                                        cc: DefaultAuthenticatedControllerComponents,
                                        returnsService: ReturnsService,
                                        checkExclusionsService: CheckExclusionsService,
                                        saveForLaterRepository: SaveForLaterRepository
                                      )(implicit ec: ExecutionContext) {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

  def listStatuses(commencementDate: LocalDate): Action[AnyContent] = cc.auth().async {
    implicit request =>
      val excludedTrader = request.registration.exclusions.toList
      val periodWithStatuses = returnsService.getStatuses(request.iossNumber, commencementDate, excludedTrader)

      periodWithStatuses.map { pws =>
        Ok(Json.toJson(pws))
      }
  }

  def getCurrentReturnsForIossNumber(iossNumber: String): Action[AnyContent] = cc.checkIossNumber(iossNumber).async {
    implicit request =>
      getResultForCurrentReturns(iossNumber)
  }

  def getCurrentReturns: Action[AnyContent] = cc.auth().async {
    implicit request =>
      getResultForCurrentReturns(request.iossNumber)
  }

  private def getResultForCurrentReturns(iossNumber: String)(implicit request: AuthorisedRequest[_]): Future[Result] = {
    for {
      availablePeriodsWithStatus <- returnsService.getStatuses(
        iossNumber,
        LocalDate.parse(request.registration.schemeDetails.commencementDate, dateTimeFormatter),
        request.registration.exclusions.toList
      )
      savedAnswers <- saveForLaterRepository.get(iossNumber)
    } yield {
      val latestAnswer = savedAnswers.sortBy(_.lastUpdated).lastOption
      val periodInProgress = latestAnswer.map(answer => answer.period)

      val incompleteReturns = createInCompleteReturns(availablePeriodsWithStatus, periodInProgress)
      val otherReturns = createCompleteReturns(availablePeriodsWithStatus, periodInProgress)

      val isExcluded = checkExclusionsService.getLastExclusionWithoutReversal(request.registration.exclusions.toList).isDefined
      val finalReturnsCompleted = returnsService.hasSubmittedFinalReturn(request.registration.exclusions.toList, availablePeriodsWithStatus)

      Ok(Json.toJson(CurrentReturns(incompleteReturns, isExcluded, finalReturnsCompleted, iossNumber, otherReturns)))
    }
  }

  private def createInCompleteReturns(availablePeriodsWithStatus: Seq[PeriodWithStatus], periodInProgress: Option[Period]) = {
    val incompletePeriods = availablePeriodsWithStatus.filterNot(pws => Seq(Complete, Excluded, Expired).contains(pws.status))
    val oldestPeriod = incompletePeriods.sortBy(_.period).headOption

    incompletePeriods.sortBy(_.period).map(
      periodWithStatus => Return.fromPeriod(
        periodWithStatus.period,
        periodWithStatus.status,
        periodInProgress.contains(periodWithStatus.period),
        oldestPeriod.contains(periodWithStatus)
      )
    )
  }

  private def createCompleteReturns(availablePeriodsWithStatus: Seq[PeriodWithStatus], periodInProgress: Option[Period]) = {
    val completePeriods = availablePeriodsWithStatus.filter(pws => Seq(Complete, Excluded, Expired).contains(pws.status))
    val oldestPeriod = completePeriods.sortBy(_.period).headOption

    completePeriods.sortBy(_.period).map(
      periodWithStatus => Return.fromPeriod(
        periodWithStatus.period,
        periodWithStatus.status,
        periodInProgress.contains(periodWithStatus.period),
        oldestPeriod.contains(periodWithStatus)
      )
    )
  }
}
