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
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.etmp.registration.{EtmpExclusion, EtmpExclusionReason}
import uk.gov.hmrc.iossreturns.models.etmp.{EtmpObligation, EtmpObligationDetails, EtmpObligations, EtmpObligationsFulfilmentStatus, EtmpVatReturn}
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
  val vatReturnConnector = mock[VatReturnConnector]

  private val mockCheckExclusionsService: CheckExclusionsService = mock[CheckExclusionsService]

  val stubClock: Clock = Clock.fixed(LocalDate.of(2022, 10, 1).atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)
  val period2021APRIL = Period(2021, Month.APRIL)
  val period2021MAY = Period(2021, Month.MAY)
  val period2021JUNE = Period(2021, Month.JUNE)
  val period2021JULY = Period(2021, Month.JULY)
  val period2021AUGUST = Period(2021, Month.AUGUST)
  val period2021SEPTEMBER = Period(2021, Month.SEPTEMBER)
  val period2022JANUARY = Period(2022, Month.JANUARY)
  val period2022FEBRUARY = Period(2022, Month.FEBRUARY)
  val period2022MARCH = Period(2022, Month.MARCH)
  val period2022APRIL = Period(2022, Month.APRIL)
  val period2022MAY = Period(2022, Month.MAY)
  val period2022JUNE = Period(2022, Month.JUNE)
  val period2022JULY = Period(2022, Month.JULY)
  val period2022AUGUST = Period(2022, Month.AUGUST)
  val period2022SEPTEMBER = Period(2022, Month.SEPTEMBER)
  val periods = Seq(
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
      (stubbedNow.minusMonths(3), stubbedNow, List(period2022SEPTEMBER, period2022AUGUST, period2022JULY), "get all periods from commencement date to date, excluding the current period because last day is not the end date of the current period"),
      (stubbedNow.minusMonths(3), stubbedNow.plusMonths(1).minusDays(1), List(period2022SEPTEMBER.getNext(), period2022SEPTEMBER, period2022AUGUST, period2022JULY), "get all periods from commencement date to end date being the last day of the period, thus including the current period"),
      (stubbedNow.plusMonths(1), stubbedNow, Nil, "return no periods if startDate is in the future, and end date is now.")
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
    val periodOverdue = currentPeriod.getPrevious().getPrevious().getPrevious()
    val excludedPeriod = currentPeriod.getNext()
    val scenarios = Table[Period, List[Period], List[EtmpExclusion], PeriodWithStatus, String](
      ("period", "vat return", "exclusions", "expected result", "title"),
      (excludedPeriod, List(currentPeriod), List(exclusion), PeriodWithStatus(excludedPeriod, SubmissionStatus.Excluded), "for a period where there is an exclusion with an effective date within previous period, mark is as excluded"),
      (currentPeriod, List(currentPeriod), Nil, PeriodWithStatus(currentPeriod, SubmissionStatus.Complete), "for a period where there is no exclusion, AND there is a vat return, mark is as Complete"),
      (currentPeriod, List.empty, Nil, PeriodWithStatus(currentPeriod, SubmissionStatus.Due), "for a period where there is no exclusion, AND there is no vat return yet, mark is as Due, if the duedate has not passed"),
      (periodOverdue, List.empty, Nil, PeriodWithStatus(periodOverdue, SubmissionStatus.Overdue), "for a period where there is no exclusion, AND there is no vat return yet, mark is as Due, if the duedate has passed")
    )

    forAll(scenarios) { (period: Period, fulfilledPeriods: List[Period], exclusions: List[EtmpExclusion], expected: PeriodWithStatus, title: String) => {
      s"when $title" in {
        service.decideStatus(period, fulfilledPeriods, exclusions) mustEqual expected
      }
      }
    }
  }

  "getStatuses" - {
    val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)
    val exclusion = EtmpExclusion(EtmpExclusionReason.FailsToComply, period2022AUGUST.firstDay, period2022AUGUST.firstDay, quarantine = false)
    val currentPeriod = Period.getRunningPeriod(stubbedNow)

    val scenarios = Table[LocalDate, List[Period], List[EtmpExclusion], List[PeriodWithStatus], String](
      ("period", "vat return", "exclusions", "expected result", "title"),
      (stubbedNow.minusMonths(2), List(period2022SEPTEMBER, period2022AUGUST), Nil, List(PeriodWithStatus(period2022SEPTEMBER, Complete), PeriodWithStatus(period2022AUGUST, Complete), PeriodWithStatus(currentPeriod, Next)), "aif all periods return vat returns(all complete) so add next for next period"),
      (stubbedNow.minusMonths(3), List(period2022JULY), List(exclusion), List(PeriodWithStatus(period2022SEPTEMBER, Excluded), PeriodWithStatus(period2022AUGUST, Excluded), PeriodWithStatus(period2022JULY, Complete)), "exclusion for next period after effective date"),
      (stubbedNow.minusMonths(3), List.empty, List(exclusion), List(PeriodWithStatus(period2022SEPTEMBER, Excluded), PeriodWithStatus(period2022AUGUST, Excluded), PeriodWithStatus(period2022JULY, Overdue)), "Overdue, if vat return can not be found for period - with exclusion"),
      (stubbedNow.minusMonths(2), List.empty, Nil, List(PeriodWithStatus(period2022SEPTEMBER, Due), PeriodWithStatus(period2022AUGUST, Overdue)), "verdue, if vat return can not be found for period - without exclusion for last month, so last month due")
    )

    forAll(scenarios) {
      (commencementLocalDate: LocalDate, fulfilledPeriods: List[Period], exclusions: List[EtmpExclusion], expected: List[PeriodWithStatus], title: String) => {
        s"when $title" in {
          val obligations = EtmpObligations(Seq(EtmpObligation(fulfilledPeriods.map { fulfilledPeriod =>
            EtmpObligationDetails(EtmpObligationsFulfilmentStatus.Fulfilled, fulfilledPeriod.toEtmpPeriodString)
          })))

          when(vatReturnConnector.getObligations(any(), any())) thenReturn Future.successful(Right(obligations))

          whenReady(service.getStatuses("iossNumber", commencementLocalDate, exclusions), Timeout(Span(2, Seconds))) {
            result =>
              result mustEqual expected
          }

        }
      }
    }
  }

  ".isPeriodExcluded" - {

    "must return true when period is excluded because hasActiveWindowExpired returns true" in {

      when(mockCheckExclusionsService.hasActiveWindowExpired(any())) thenReturn true

      val period = Period(stubbedNow.getYear, stubbedNow.getMonth)
      val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.plusMonths(1)))

      val service = new ReturnsService(stubClockAtArbitraryDate, vatReturnConnector, mockCheckExclusionsService)

      val result = service.isPeriodExcluded(period, exclusion)

      result mustBe true
    }

    "must return true when period is excluded because hasActiveWindowExpired returns false and the period falls after the exclusion effective date" in {

      when(mockCheckExclusionsService.hasActiveWindowExpired(any())) thenReturn false

      val period = Period(stubbedNow.getYear, stubbedNow.getMonth)

      val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.minusMonths(1)))

      val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

      val result = service.isPeriodExcluded(period, exclusion)

      result mustBe true
    }

    "must return true when period is excluded because hasActiveWindowExpired returns false and the period falls on the exclusion effective date" in {

      when(mockCheckExclusionsService.hasActiveWindowExpired(any())) thenReturn false

      val period = Period(stubbedNow.getYear, stubbedNow.getMonth)

      val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = period.firstDay))

      val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

      val result = service.isPeriodExcluded(period, exclusion)

      result mustBe true
    }

    "must return true when period is excluded because hasActiveWindowExpired returns true and the period falls after the exclusion effective date" in {

      when(mockCheckExclusionsService.hasActiveWindowExpired(any())) thenReturn true

      val period = Period(stubbedNow.getYear, stubbedNow.getMonth)
      val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.minusMonths(1)))

      val service = new ReturnsService(stubClockAtArbitraryDate, vatReturnConnector, mockCheckExclusionsService)

      val result = service.isPeriodExcluded(period, exclusion)

      result mustBe true
    }

    "must return false when period is not excluded because hasActiveWindowExpired returns false and the period falls before the exclusion effective date" in {

      when(mockCheckExclusionsService.hasActiveWindowExpired(any())) thenReturn false

      val period = Period(stubbedNow.getYear, stubbedNow.getMonth)
      val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.plusMonths(1)))

      val service = new ReturnsService(stubClockAtArbitraryDate, vatReturnConnector, mockCheckExclusionsService)

      val result = service.isPeriodExcluded(period, exclusion)

      result mustBe false
    }

    "must return false when there is no exclusion" in {

      val period = Period(stubbedNow.getYear, stubbedNow.getMonth)
      val exclusion = List.empty

      val service = new ReturnsService(stubClockAtArbitraryDate, vatReturnConnector, mockCheckExclusionsService)

      val result = service.isPeriodExcluded(period, exclusion)

      result mustBe false
    }
  }

  "general period oss response behaviour" - {

    "when today is 11th October" - {

      val instant = Instant.ofEpochSecond(1633959834)
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      "should return September for commencement date of 30th September (comencement date as the last day of period)" in {
        val commencementDate = LocalDate.of(2021, 9, 30)
        val service = new ReturnsService(stubClock, vatReturnConnector, mockCheckExclusionsService)

        val expectedPeriods = Seq(Period(2021, Month.SEPTEMBER))
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

      val expectedPeriods = Seq(Period(2021, Month.JULY), Period(2021, Month.AUGUST), Period(2021, Month.SEPTEMBER))

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

      val expectedPeriods = Seq(Period(2021, Month.OCTOBER), Period(2021, Month.NOVEMBER), Period(2021, Month.DECEMBER))

      val returnValue: Future[Seq[PeriodWithStatus]] = service.getStatuses("iossNumber", LocalDate.now(stubClock).minusMonths(3), Nil)

      whenReady(returnValue, Timeout(Span(2, Seconds))) {
        statuses: Seq[PeriodWithStatus] =>
          statuses.map(_.period) must contain theSameElementsAs expectedPeriods
      }
    }
  }
}