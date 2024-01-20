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
import uk.gov.hmrc.iossreturns.models.{EtmpDisplayReturnError, Period}
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusionReason.Reversal
import uk.gov.hmrc.iossreturns.models.etmp.EtmpVatReturn
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusion
import uk.gov.hmrc.iossreturns.models.youraccount.{PeriodWithStatus, SubmissionStatus}

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class ReturnsService @Inject()(clock: Clock, vatReturnConnector: VatReturnConnector)(implicit executionContext: ExecutionContext) {
  private def getReturn(iossNumber: String, period: Period): Future[Option[EtmpVatReturn]] = {
    val result = vatReturnConnector.get(iossNumber, period)
    result.flatMap {
      case Right(r) => Future.successful(Some(r))
      case Left(EtmpDisplayReturnError(code, _)) if code startsWith "UNEXPECTED_404" => Future.successful(None)
      case Left(errorResponse) => Future.failed(new Exception(errorResponse.body))
    }
  }

  def getStatuses(
                   iossNumber: String,
                   commencementLocalDate: LocalDate,
                   exclusions: List[EtmpExclusion]
                 ): Future[Seq[PeriodWithStatus]] = {
    val vatReturnsResult: Seq[Future[(Period, Option[EtmpVatReturn])]] =
      getAllPeriodsBetween(commencementLocalDate, LocalDate.now(clock)).map(period =>
        getReturn(iossNumber, period).map((period, _))
      )
    val vatReturns: Future[Seq[(Period, Option[EtmpVatReturn])]] = Future.sequence(vatReturnsResult)
    vatReturns.map(periodAndReturns => {
      val allPeriodAndReturns = periodAndReturns.map(periodAndReturn => {
        val (period, vatReturn) = periodAndReturn
        val decision: PeriodWithStatus = decideStatus(period, vatReturn, exclusions)
        decision
      })
      addNextIfAllCompleted(allPeriodAndReturns.toList, commencementLocalDate)
    })
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

  def decideStatus(period: Period, vatReturn: Option[EtmpVatReturn], exclusions: List[EtmpExclusion]) = {
    if (isPeriodExcluded(period, exclusions))
      PeriodWithStatus(period, SubmissionStatus.Excluded)
    else
      vatReturn match {
        case Some(_) => PeriodWithStatus(period, SubmissionStatus.Complete)
        case None => if (LocalDate.now(clock).isAfter(period.paymentDeadline)) {
          PeriodWithStatus(period, SubmissionStatus.Overdue)
        } else {
          PeriodWithStatus(period, SubmissionStatus.Due)
        }
      }
  }

  def getLastExclusionWithoutReversal(exclusions: List[EtmpExclusion]): Option[EtmpExclusion] = {

      exclusions.maxByOption(_.effectiveDate).filterNot(_.exclusionReason == Reversal)

  }

  def isPeriodExcluded(period: Period, exclusions: List[EtmpExclusion]): Boolean = {
    val excluded = getLastExclusionWithoutReversal(exclusions)
    excluded match {
      case Some(excluded) if period.lastDay.isAfter(Period.getRunningPeriod(excluded.effectiveDate).getNext().firstDay) =>
        true
      case _ => false
    }
  }

  def hasSubmittedFinalReturn(iossNumber: String, exclusions: List[EtmpExclusion])(implicit executionContext: ExecutionContext): Future[Boolean] = {
    getLastExclusionWithoutReversal(exclusions) match {
      case Some(EtmpExclusion(_, _, effectiveDate, _)) =>
        getReturn(iossNumber, Period.getRunningPeriod(effectiveDate)).map {
          case Some(_) => true
          case _ => false
        }
      case _ => Future.successful(false)
    }
  }
}
