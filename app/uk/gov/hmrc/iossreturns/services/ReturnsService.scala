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

package uk.gov.hmrc.iossreturns.services

import uk.gov.hmrc.iossreturns.connectors.VatReturnConnector
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models.Period.{getNext, getPrevious}
import uk.gov.hmrc.iossreturns.models.{Period, StandardPeriod}
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligationsQueryParameters
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusion
import uk.gov.hmrc.iossreturns.models.youraccount.{PeriodWithStatus, SubmissionStatus}
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnsService @Inject()(
                                clock: Clock,
                                vatReturnConnector: VatReturnConnector,
                                checkExclusionsService: CheckExclusionsService
                              )(implicit executionContext: ExecutionContext) extends Logging {

  def getStatuses(
                   iossNumber: String,
                   commencementLocalDate: LocalDate,
                   exclusions: List[EtmpExclusion]
                 ): Future[Seq[PeriodWithStatus]] = {
    val today = LocalDate.now(clock)

    val periods = getAllPeriodsBetween(commencementLocalDate, today)
    val etmpObligationsQueryParameters = EtmpObligationsQueryParameters(
      fromDate = commencementLocalDate.format(etmpDateFormatter),
      toDate = today.plusMonths(1).withDayOfMonth(1).minusDays(1).format(etmpDateFormatter),
      None
    )
    val futureFulfilledPeriods = vatReturnConnector
      .getObligations(iossNumber, etmpObligationsQueryParameters).map {
        case Right(obligations) => obligations.getFulfilledPeriods
        case x =>
          logger.error(s"Error when getting obligations for return status' $x")
          throw new Exception("Error getting obligations for status'")
      }

    for {
      fulfilledPeriods <- futureFulfilledPeriods
    } yield {
      val allPeriodsAndReturns = periods.map { period =>
        decideStatus(period, fulfilledPeriods, exclusions)
      }
      addNextIfAllCompleted(allPeriodsAndReturns, commencementLocalDate)
    }
  }

  private def addNextIfAllCompleted(currentPeriods: List[PeriodWithStatus], commencementLocalDate: LocalDate): List[PeriodWithStatus] = {
    val nextPeriod = getNextPeriod(currentPeriods.map(_.period), commencementLocalDate)
    if (currentPeriods.forall(_.status == SubmissionStatus.Complete)) {
      currentPeriods ++ Seq(PeriodWithStatus(nextPeriod, SubmissionStatus.Next))
    } else {
      currentPeriods
    }
  }

  def getNextPeriod(periods: List[Period], commencementLocalDate: LocalDate): Period = {
    val runningPeriod = Period.getRunningPeriod(LocalDate.now(clock))
    if (periods.nonEmpty) {
      getNext(periods.maxBy(_.lastDay.toEpochDay))
    } else {
      if (commencementLocalDate.isAfter(runningPeriod.lastDay)) {
        Period.getRunningPeriod(commencementLocalDate)
      } else {
        runningPeriod
      }
    }

  }

  def getAllPeriodsBetween(commencementDate: LocalDate, endDate: LocalDate): List[Period] = {
    val startPeriod = StandardPeriod(commencementDate.getYear, commencementDate.getMonth)
    getPeriodsUntilDate(startPeriod, endDate)
  }

  private def getPeriodsUntilDate(currentPeriod: Period, endDate: LocalDate): List[Period] = {
    if (currentPeriod.lastDay.isBefore(endDate)) {
      List(currentPeriod) ++ getPeriodsUntilDate(getNext(currentPeriod), endDate)
    } else {
      List.empty
    }
  }

  def decideStatus(period: Period, fulfilledPeriods: List[Period], exclusions: List[EtmpExclusion]): PeriodWithStatus = {
    if (checkExclusionsService.isPeriodExcluded(period, exclusions)) {
      PeriodWithStatus(period, SubmissionStatus.Excluded)
    } else {
      if (fulfilledPeriods.contains(period)) {
        PeriodWithStatus(period, SubmissionStatus.Complete)
      } else {
        if (LocalDate.now(clock).isAfter(period.paymentDeadline)) {
          PeriodWithStatus(period, SubmissionStatus.Overdue)
        } else {
          PeriodWithStatus(period, SubmissionStatus.Due)
        }
      }
    }
  }

  def hasSubmittedFinalReturn(exclusions: List[EtmpExclusion], periodsWithStatus: Seq[PeriodWithStatus]): Boolean = {
    checkExclusionsService.getLastExclusionWithoutReversal(exclusions) match {
      case Some(EtmpExclusion(_, _, effectiveDate, _)) =>
        periodsWithStatus
          .exists { periodWithStatus =>

            val runningPeriod = Period.getRunningPeriod(effectiveDate)

            val periodToCheck = if (runningPeriod.firstDay == effectiveDate) {
              getPrevious(runningPeriod)
            } else {
              runningPeriod
            }
            periodWithStatus.period == periodToCheck &&
              periodWithStatus.status == SubmissionStatus.Complete
          }
      case _ => false
    }
  }
}
