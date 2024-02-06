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
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusionReason.Reversal
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligationsQueryParameters
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusion
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligationsFulfilmentStatus.Fulfilled
import uk.gov.hmrc.iossreturns.models.youraccount.{PeriodWithStatus, SubmissionStatus}
import uk.gov.hmrc.iossreturns.utils.Formatters.etmpDateFormatter

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class ReturnsService @Inject()(clock: Clock, vatReturnConnector: VatReturnConnector)(implicit executionContext: ExecutionContext) extends Logging {

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
      Some(Fulfilled.toString)
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
      periods.maxBy(_.lastDay.toEpochDay).getNext()
    } else {
      if (commencementLocalDate.isAfter(runningPeriod.lastDay)) {
        Period.getRunningPeriod(commencementLocalDate)
      } else {
        runningPeriod
      }
    }

  }

  def getAllPeriodsBetween(commencementDate: LocalDate, endDate: LocalDate): List[Period] = {
    val startPeriod = Period(commencementDate.getYear, commencementDate.getMonth)
    val endPeriod = Period(endDate.getYear, endDate.getMonth)

    val lastPeriod = if (endDate.isBefore(endPeriod.lastDay))
      endPeriod.getPrevious()
    else
      endPeriod

    getAllPeriodsUntil(Nil, startPeriod, lastPeriod)
  }

  @tailrec
  private def getAllPeriodsUntil(periodsUpToNow: List[Period], startPeriod: Period, endPeriod: Period): List[Period] = {
    if (endPeriod.isBefore(startPeriod))
      Nil
    else if (startPeriod == endPeriod)
      startPeriod :: periodsUpToNow
    else
      getAllPeriodsUntil(startPeriod :: periodsUpToNow, startPeriod.getNext(), endPeriod)
  }

  def decideStatus(period: Period, fulfilledPeriods: List[Period], exclusions: List[EtmpExclusion]): PeriodWithStatus = {
    if (isPeriodExcluded(period, exclusions))
      PeriodWithStatus(period, SubmissionStatus.Excluded)
    else {
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

  def getLastExclusionWithoutReversal(exclusions: List[EtmpExclusion]): Option[EtmpExclusion] = {
    // Even though API is array ETMP only return single item
    exclusions.headOption.filterNot(_.exclusionReason == Reversal)
  }

  def isPeriodExcluded(period: Period, exclusions: List[EtmpExclusion]): Boolean = {
    val excluded = getLastExclusionWithoutReversal(exclusions)
    excluded match {
      case Some(excluded) if period.lastDay.isAfter(Period.getRunningPeriod(excluded.effectiveDate).getNext().firstDay) =>
        true
      case _ => false
    }
  }

  def hasSubmittedFinalReturn(exclusions: List[EtmpExclusion], periodsWithStatus: Seq[PeriodWithStatus]): Boolean = {
    getLastExclusionWithoutReversal(exclusions) match {
      case Some(EtmpExclusion(_, _, effectiveDate, _)) =>
        periodsWithStatus
          .exists(periodWithStatus =>
            periodWithStatus.period == Period.getRunningPeriod(effectiveDate) &&
              periodWithStatus.status == SubmissionStatus.Complete
          )
      case _ => false
    }
  }
}
