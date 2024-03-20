/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.VatReturnConnector
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models.Period.{getNext, getPrevious, toEtmpPeriodString}
import uk.gov.hmrc.iossreturns.models.{Period, StandardPeriod}
import uk.gov.hmrc.iossreturns.models.etmp.registration.{EtmpExclusion, EtmpExclusionReason}
import uk.gov.hmrc.iossreturns.models.etmp._
import uk.gov.hmrc.iossreturns.models.youraccount.SubmissionStatus.{Complete, Due, Excluded, Next, Overdue}
import uk.gov.hmrc.iossreturns.models.youraccount.{PeriodWithStatus, SubmissionStatus}

import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReturnsServiceSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with IntegrationPatience
    with BeforeAndAfterEach {
  val vatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  private val mockCheckExclusionsService: CheckExclusionsService = mock[CheckExclusionsService]

  val stubClock: Clock = Clock.fixed(LocalDate.of(2022, 10, 1).atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)
  val period2021APRIL: StandardPeriod = StandardPeriod(2021, Month.APRIL)
  val period2021MAY: StandardPeriod = StandardPeriod(2021, Month.MAY)
  val period2021JUNE: StandardPeriod = StandardPeriod(2021, Month.JUNE)
  val period2021JULY: StandardPeriod = StandardPeriod(2021, Month.JULY)
  val period2021AUGUST: StandardPeriod = StandardPeriod(2021, Month.AUGUST)
  val period2021SEPTEMBER: StandardPeriod = StandardPeriod(2021, Month.SEPTEMBER)
  val period2022JANUARY: StandardPeriod = StandardPeriod(2022, Month.JANUARY)
  val period2022FEBRUARY: StandardPeriod = StandardPeriod(2022, Month.FEBRUARY)
  val period2022MARCH: StandardPeriod = StandardPeriod(2022, Month.MARCH)
  val period2022APRIL: StandardPeriod = StandardPeriod(2022, Month.APRIL)
  val period2022MAY: StandardPeriod = StandardPeriod(2022, Month.MAY)
  val period2022JUNE: StandardPeriod = StandardPeriod(2022, Month.JUNE)
  val period2022JULY: StandardPeriod = StandardPeriod(2022, Month.JULY)
  val period2022AUGUST: StandardPeriod = StandardPeriod(2022, Month.AUGUST)
  val period2022SEPTEMBER: StandardPeriod = StandardPeriod(2022, Month.SEPTEMBER)
  val periods: Seq[StandardPeriod] = Seq(
    period2021APRIL,
    period2021MAY,
    period2021JUNE,
    period2021JULY,
    period2021AUGUST,
    period2021SEPTEMBER,
    period2022JANUARY,
    period2022FEBRUARY,
    period2022MARCH,
    period2022APRIL,
    period2022MAY,
    period2022JUNE,
    period2022JULY,
    period2022AUGUST,
    period2022SEPTEMBER
  )

  val vatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.get
  when(vatReturnConnector.get(any(), any())).thenReturn(Future.successful(Right(vatReturn)))

  val stubbedNow: LocalDate = LocalDate.now(stubClock)

  override def beforeEach(): Unit = {
    Mockito.reset(mockCheckExclusionsService)
  }

  "getAllPeriodsBetween" - {
    val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)
    val scenarios = Table[LocalDate, LocalDate, List[Period], String](
      ("startDate", "endDate", "expected result", "title"),
      (stubbedNow.minusMonths(3), stubbedNow, List(period2022JULY, period2022AUGUST, period2022SEPTEMBER), "get all periods from commencement date to date, excluding the current period because last day is not the end date of the current period"),
      (stubbedNow.minusMonths(3), stubbedNow.plusMonths(1).withDayOfMonth(1).minusDays(1), List(period2022JULY, period2022AUGUST, period2022SEPTEMBER), "get all periods from commencement date to end date being the last day of the period, thus including the current period"),
      (stubbedNow.plusMonths(1), stubbedNow, Nil, "return no periods if startDate is in the future, and end date is now."),
      (LocalDate.of(2024, 2, 10), LocalDate.of(2024, 2, 29), Nil, "Return empty when today is a leap day and commencement day is a leap day")
    )


    forAll(scenarios) { (startDate: LocalDate, endDate: LocalDate, expected: List[Period], title) => {
      s"$title" in {
        service.getAllPeriodsBetween(startDate, endDate) mustEqual expected
      }
    }
    }
  }

  "getNextPeriod" - {
    val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)
    val canIgnore = stubbedNow
    val currentPeriod = Period.getRunningPeriod(stubbedNow)
    val commencementDateBeforeCurrentPeriodsLastDay = currentPeriod.lastDay.minusDays(1)
    val commencementDateAfterCurrentPeriodsLastDay = currentPeriod.lastDay.plusDays(1)
    val commencementDatesPeriod = Period.getRunningPeriod(commencementDateAfterCurrentPeriodsLastDay)
    val scenarios = Table[LocalDate, List[Period], Period, String](
      ("commencementDate", "periods", "expected result", "title"),
      (canIgnore, List(period2021MAY, period2022AUGUST, period2022JULY), period2022SEPTEMBER, "get the next of maximum period when the list is not empty"),
      (commencementDateBeforeCurrentPeriodsLastDay, Nil, currentPeriod, "get the current period when commencement date is before the last day of the current period AND the period list IS EMPTY"),
      (commencementDateAfterCurrentPeriodsLastDay, Nil, commencementDatesPeriod, "get the commencement period when commencement date is after the last day of the current period AND the period list IS EMPTY")
    )

    forAll(scenarios) { (commencementDate: LocalDate, periods: List[Period], expected: Period, title: String) => {
      s"when $title" in {
        service.getNextPeriod(periods, commencementDate) mustEqual expected
      }
    }
    }
  }

  "decideStatus" - {
    val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)
    val exclusion = EtmpExclusion(EtmpExclusionReason.FailsToComply, stubbedNow, stubbedNow.minusMonths(1), quarantine = false)
    val currentPeriod = Period.getRunningPeriod(stubbedNow)
    val periodOverdue = getPrevious(getPrevious(getPrevious(currentPeriod)))
    val excludedPeriod = getNext(currentPeriod)
    val scenarios = Table[Period, List[Period], List[Period], List[EtmpExclusion], PeriodWithStatus, String](
      ("period", "vat return", "excludedPeriods", "exclusions", "expected result", "title"),
      (excludedPeriod, List(currentPeriod), List(excludedPeriod), List(exclusion), PeriodWithStatus(excludedPeriod, SubmissionStatus.Excluded), "for a period where there is an exclusion with an effective date within previous period, mark is as excluded"),
      (currentPeriod, List(currentPeriod), Nil, Nil, PeriodWithStatus(currentPeriod, SubmissionStatus.Complete), "for a period where there is no exclusion, AND there is a vat return, mark is as Complete"),
      (currentPeriod, List.empty, Nil, Nil, PeriodWithStatus(currentPeriod, SubmissionStatus.Due), "for a period where there is no exclusion, AND there is no vat return yet, mark is as Due, if the duedate has not passed"),
      (periodOverdue, List.empty, Nil, Nil, PeriodWithStatus(periodOverdue, SubmissionStatus.Overdue), "for a period where there is no exclusion, AND there is no vat return yet, mark is as Due, if the duedate has passed")
    )

    forAll(scenarios) {
      (period: Period, fulfilledPeriods: List[Period], excludedPeriods: List[Period], exclusions: List[EtmpExclusion], expected: PeriodWithStatus, title: String) => {
        s"when $title" in {
          for (ep <- excludedPeriods) {
            when(mockCheckExclusionsService.isPeriodExcluded(ep, exclusions)) thenReturn true
          }
        service.decideStatus(period, fulfilledPeriods, exclusions) mustEqual expected
      }
      }
    }
  }

  "getStatuses" - {
    val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)
    val exclusion = EtmpExclusion(EtmpExclusionReason.FailsToComply, period2022AUGUST.firstDay, period2022AUGUST.firstDay, quarantine = false)
    val currentPeriod = Period.getRunningPeriod(stubbedNow)

    val scenarios = Table[LocalDate, List[Period], List[Period], List[EtmpExclusion], List[PeriodWithStatus], String](
      ("period", "vat return", "excludedPeriods", "exclusions", "expected result", "title"),
      (stubbedNow.minusMonths(2), List(period2022AUGUST, period2022SEPTEMBER), Nil, Nil, List(PeriodWithStatus(period2022AUGUST, Complete), PeriodWithStatus(period2022SEPTEMBER, Complete), PeriodWithStatus(currentPeriod, Next)), "aif all periods return vat returns(all complete) so add next for next period"),
      (stubbedNow.minusMonths(3), List(period2022JULY), List(period2022AUGUST, period2022SEPTEMBER), List(exclusion), List(PeriodWithStatus(period2022JULY, Complete), PeriodWithStatus(period2022AUGUST, Excluded), PeriodWithStatus(period2022SEPTEMBER, Excluded)), "exclusion for next period after effective date"),
      (stubbedNow.minusMonths(3), List.empty, List(period2022AUGUST, period2022SEPTEMBER), List(exclusion), List(PeriodWithStatus(period2022JULY, Overdue), PeriodWithStatus(period2022AUGUST, Excluded), PeriodWithStatus(period2022SEPTEMBER, Excluded)), "Overdue, if vat return can not be found for period - with exclusion"),
      (stubbedNow.minusMonths(2), List.empty, Nil, Nil, List(PeriodWithStatus(period2022AUGUST, Overdue), PeriodWithStatus(period2022SEPTEMBER, Due)), "verdue, if vat return can not be found for period - without exclusion for last month, so last month due")
    )

    forAll(scenarios) {
      (commencementLocalDate: LocalDate, fulfilledPeriods: List[Period], excludedPeriods: List[Period], exclusions: List[EtmpExclusion], expected: List[PeriodWithStatus], title: String) => {
        s"when $title" in {
          val obligations = EtmpObligations(Seq(EtmpObligation(fulfilledPeriods.map { fulfilledPeriod =>
            EtmpObligationDetails(EtmpObligationsFulfilmentStatus.Fulfilled, toEtmpPeriodString(fulfilledPeriod))
          })))

          when(vatReturnConnector.getObligations(any(), any())) thenReturn Future.successful(Right(obligations))

          for (ep <- excludedPeriods) {
            when(mockCheckExclusionsService.isPeriodExcluded(ep, exclusions)) thenReturn true
          }

          whenReady(service.getStatuses("iossNumber", commencementLocalDate, exclusions), Timeout(Span(2, Seconds))) {
            result =>
              result mustEqual expected
          }
        }
      }
    }
  }

  "general period oss response behaviour" - {

    "when today is 11th October" - {

      val instant = Instant.ofEpochSecond(1633959834)
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      "should return September for commencement date of 30th September (comencement date as the last day of period)" in {
        val commencementDate = LocalDate.of(2021, 9, 30)
        val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

        val expectedPeriods = Seq(StandardPeriod(2021, Month.SEPTEMBER))
        val returnValue: Future[Seq[PeriodWithStatus]] = service.getStatuses("iossNumber", commencementDate, Nil)

        whenReady(returnValue, Timeout(Span(2, Seconds))) { statuses: Seq[PeriodWithStatus] =>
          statuses.map(_.period) must contain theSameElementsAs expectedPeriods
        }
      }

      "should return nothing for commencement date of 10th October (filter Next)" in {
        val commencementDate = LocalDate.of(2021, 10, 10)

        val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

        val expectedPeriods = Seq.empty

        val returnValue: Future[Seq[PeriodWithStatus]] = service.getStatuses("iossNumber", commencementDate, Nil)

        whenReady(returnValue, Timeout(Span(2, Seconds))) { statuses: Seq[PeriodWithStatus] =>
          statuses.filterNot(_.status == Next).map(_.period) must contain theSameElementsAs expectedPeriods
        }
      }
    }

    "when today is 11th October" in {
      val instant = Instant.ofEpochSecond(1633959834)
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

      val expectedPeriods = Seq(StandardPeriod(2021, Month.JULY), StandardPeriod(2021, Month.AUGUST), StandardPeriod(2021, Month.SEPTEMBER))

      val returnValue: Future[Seq[PeriodWithStatus]] = service.getStatuses("iossNumber", LocalDate.now(stubClock).minusMonths(3), Nil)


      whenReady(returnValue, Timeout(Span(2, Seconds))) {
        statuses: Seq[PeriodWithStatus] =>
          statuses.map(_.period) must contain theSameElementsAs expectedPeriods
      }
    }

    "when today is 11th January" in {
      val instant = Instant.parse("2022-01-11T12:00:00Z")

      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

      val expectedPeriods = Seq(StandardPeriod(2021, Month.OCTOBER), StandardPeriod(2021, Month.NOVEMBER), StandardPeriod(2021, Month.DECEMBER))

      val returnValue: Future[Seq[PeriodWithStatus]] = service.getStatuses("iossNumber", LocalDate.now(stubClock).minusMonths(3), Nil)

      whenReady(returnValue, Timeout(Span(2, Seconds))) {
        statuses: Seq[PeriodWithStatus] =>
          statuses.map(_.period) must contain theSameElementsAs expectedPeriods
      }
    }
  }
}