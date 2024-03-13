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

package uk.gov.hmrc.iossreturns.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.controllers.actions._
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models.{Period, StandardPeriod}
import uk.gov.hmrc.iossreturns.models.etmp.registration.{EtmpExclusion, EtmpExclusionReason}
import uk.gov.hmrc.iossreturns.models.youraccount.SubmissionStatus.{Complete, Due, Excluded, Next, Overdue}
import uk.gov.hmrc.iossreturns.models.youraccount.{CurrentReturns, PeriodWithStatus, Return, SubmissionStatus}
import uk.gov.hmrc.iossreturns.services.{CheckExclusionsService, ReturnsService}

import java.time.{Clock, LocalDate, Month, ZoneId}
import scala.concurrent.Future

class ReturnStatusControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with Generators {
  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].to[FakeAuthAction],
        bind[CheckOwnIossNumberFilter].to[FakeCheckOwnIossNumberFilterProvider]
      )

  private val completeOrExcludedStatuses: Seq[SubmissionStatus] = Seq(Complete, Excluded)

  private val mockCheckExclusionsService: CheckExclusionsService = mock[CheckExclusionsService]

  ".getCurrentReturnsForIossNumber()" - {
    val stubClock: Clock = Clock.fixed(LocalDate.of(2022, 10, 1).atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)
    val period2021APRIL = StandardPeriod(2021, Month.APRIL)
    val period2021MAY = StandardPeriod(2021, Month.MAY)
    val period2021JUNE = StandardPeriod(2021, Month.JUNE)
    val period2021JULY = StandardPeriod(2021, Month.JULY)
    val period2021AUGUST = StandardPeriod(2021, Month.AUGUST)
    val period2021SEPTEMBER = StandardPeriod(2021, Month.SEPTEMBER)
    val period2022JANUARY = StandardPeriod(2022, Month.JANUARY)
    val period2022FEBRUARY = StandardPeriod(2022, Month.FEBRUARY)
    val period2022MARCH = StandardPeriod(2022, Month.MARCH)
    val period2022APRIL = StandardPeriod(2022, Month.APRIL)
    val period2022MAY = StandardPeriod(2022, Month.MAY)
    val period2022JUNE = StandardPeriod(2022, Month.JUNE)
    val period2022JULY = StandardPeriod(2022, Month.JULY)
    val period2022AUGUST = StandardPeriod(2022, Month.AUGUST)
    val period2022SEPTEMBER = StandardPeriod(2022, Month.SEPTEMBER)
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

    lazy val request = FakeRequest(GET, routes.ReturnStatusController.getCurrentReturnsForIossNumber(iossNumber).url)
    "must respond with OK and the OpenReturns model" - {

      "with no returns in progress, due or overdue if there are no returns due yet" in {
        val mockReturnService = mock[ReturnsService]
        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(Seq.empty)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(Seq(
          ), excluded = false, finalReturnsCompleted = false, iossNumber,
            Seq.empty))
        }
      }

      "with no returns in progress, due or overdue if all returns are complete" in {
        val mockReturnService = mock[ReturnsService]
        val lastPeriod = periods.takeRight(1).head
        val periodsWithStatuses = periods.dropRight(1).map(PeriodWithStatus(_, Complete)).toList ::: List(PeriodWithStatus(lastPeriod, Next))

        val completeOrExcludedReturns: List[Return] = convertPeriodsWithStatusesToCompleteOrExcludedReturns(periodsWithStatuses)

        // Check we are not doing an empty list and in fact negating the value of the test, stranger things have happened
        completeOrExcludedReturns.length mustBe 14

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK

          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            returns = Seq(Return.fromPeriod(lastPeriod, Next, inProgress = false, isOldest = true)),
            excluded = false,
            finalReturnsCompleted = false,
            iossNumber = iossNumber,
            completeOrExcludedReturns = completeOrExcludedReturns
          ))
        }
      }

      def convertPeriodsWithStatusesToCompleteOrExcludedReturns(periodsWithStatuses: List[PeriodWithStatus]): List[Return] = {
        val sortedPeriodsWithStatuses = periodsWithStatuses.sortBy(_.period)

        val completeOrExcludedPeriodsWithStatuses =
          sortedPeriodsWithStatuses.filter(periodsWithStatus => completeOrExcludedStatuses.contains(periodsWithStatus.status))

        val completeOrExcludedReturns = completeOrExcludedPeriodsWithStatuses.zipWithIndex.map { case (periodWithStatus, index) =>
          val isOldest = index == 0
          Return.fromPeriod(periodWithStatus.period, periodWithStatus.status, inProgress = false, isOldest = isOldest)
        }

        completeOrExcludedReturns
      }

      "with a return due but not in progress if there's one return due but no saved answers" in {
        val mockReturnService = mock[ReturnsService]

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          List(PeriodWithStatus(period2022SEPTEMBER, Due))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            Seq(Return.fromPeriod(period2022SEPTEMBER, Due, inProgress = false, isOldest = true)),
            excluded = false,
            finalReturnsCompleted = false,
            iossNumber,
            Seq.empty
          ))
        }
      }

      "with some overdue returns but nothing in progress" in {
        val periods = Seq(period2021APRIL,
          period2021MAY,
          period2021JUNE)

        val mockReturnService = mock[ReturnsService]
        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periods.map(PeriodWithStatus(_, Overdue)))
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        val returns = Return.fromPeriod(periods.head, Overdue, inProgress = false, isOldest = true) ::
          periods.tail.map(Return.fromPeriod(_, Overdue, inProgress = false, isOldest = false)).toList

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            returns, excluded = false, finalReturnsCompleted = false, iossNumber, Seq.empty))
        }
      }

      "with a return due and some returns overdue and nothing in progress" in {
        val period1 :: periodsInBetween = periods.dropRight(1)
        val lastPeriod = periods.takeRight(1).head

        val mockReturnService = mock[ReturnsService]

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          (period1 :: periodsInBetween).map(PeriodWithStatus(_, Overdue))
            :::
            List(PeriodWithStatus(lastPeriod, Due))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(
            CurrentReturns(
              Return.fromPeriod(period1, Overdue, inProgress = false, isOldest = true) ::
                periodsInBetween.map(Return.fromPeriod(_, Overdue, inProgress = false, isOldest = false))
                :::
                List(Return.fromPeriod(lastPeriod, Due, inProgress = false, isOldest = false)),
              excluded = false,
              finalReturnsCompleted = false,
              iossNumber = iossNumber,
              Seq.empty
            ))
        }

      }

      "with an excluded trader's final return due but not in progress and no saved answers" in {

        val mockReturnService = mock[ReturnsService]

        val exclusion: Option[EtmpExclusion] = arbitraryEtmpExclusion.arbitrary.sample.map(
          _.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = period2022AUGUST.firstDay)
        )

        val periodsWithStatuses = List(PeriodWithStatus(period2022AUGUST, Overdue), PeriodWithStatus(period2022SEPTEMBER, Excluded))

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn exclusion

        val completeOfExcludedReturns = convertPeriodsWithStatusesToCompleteOrExcludedReturns(periodsWithStatuses)
        completeOfExcludedReturns.size mustBe 1

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            Seq(
              Return.fromPeriod(period2022AUGUST, Overdue, inProgress = false, isOldest = true)
            ),
            excluded = true,
            finalReturnsCompleted = false,
            iossNumber = iossNumber,
            completeOrExcludedReturns = completeOfExcludedReturns
          ))
        }
      }

      "with an excluded trader's final return completed and can't start any more returns" in {
        val mockReturnService = mock[ReturnsService]

        val exclusion: Option[EtmpExclusion] = arbitraryEtmpExclusion.arbitrary.sample.map(_.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = period2022AUGUST.firstDay))

        val periodsWithStatuses = List(PeriodWithStatus(period2022AUGUST, Complete), PeriodWithStatus(period2022SEPTEMBER, Excluded))
        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn true
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn exclusion

        val completeOfExcludedReturns = convertPeriodsWithStatusesToCompleteOrExcludedReturns(periodsWithStatuses)
        completeOfExcludedReturns.size mustBe 2

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            Seq.empty,
            excluded = true,
            finalReturnsCompleted = true,
            iossNumber = iossNumber,
            completeOrExcludedReturns = completeOfExcludedReturns
          ))
        }
      }

      "excluded trader can't complete a return 3 years after return due date" in {

        val exclusionPeriod = StandardPeriod(2023, Month.NOVEMBER)
        val stubClock: Clock = Clock.fixed(LocalDate.of(2026, 3, 1).atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)

        val mockReturnService = mock[ReturnsService]

        val exclusion: Option[EtmpExclusion] = arbitraryEtmpExclusion.arbitrary.sample.map(_.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = exclusionPeriod.firstDay))

        val periodsWithStatuses = List(
          PeriodWithStatus(StandardPeriod(2023, Month.DECEMBER), Excluded),
          PeriodWithStatus(StandardPeriod(2023, Month.JANUARY), Excluded)
        )

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses )

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn exclusion

        val completeOfExcludedReturns = convertPeriodsWithStatusesToCompleteOrExcludedReturns(periodsWithStatuses)
        completeOfExcludedReturns.size mustBe 2

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
           contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            Seq.empty,
            excluded = true,
            finalReturnsCompleted = false,
            iossNumber = iossNumber,
            completeOrExcludedReturns = completeOfExcludedReturns
          ))
        }
      }

      "excluded trader can complete a return if return due date is within 3 years" in {

        val exclusionPeriod = StandardPeriod(2023, Month.NOVEMBER)
        val stubClock: Clock = Clock.fixed(LocalDate.of(2026, 3, 1).atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)

        val mockReturnService = mock[ReturnsService]

        val exclusion: Option[EtmpExclusion] = arbitraryEtmpExclusion.arbitrary.sample.map(_.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = exclusionPeriod.firstDay))

        val vatReturns = Seq(Return.fromPeriod(StandardPeriod(2023, Month.JULY), Overdue, inProgress = false, isOldest = true))

        val periodsWithStatuses = List(
          PeriodWithStatus(StandardPeriod(2023, Month.DECEMBER), Excluded),
          PeriodWithStatus(StandardPeriod(2023, Month.JULY), Overdue),
          PeriodWithStatus(StandardPeriod(2023, Month.JANUARY), Excluded)
        )

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses)

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periodsWithStatuses)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn exclusion

        val completeOfExcludedReturns = convertPeriodsWithStatusesToCompleteOrExcludedReturns(periodsWithStatuses)
        completeOfExcludedReturns.size mustBe 2

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            vatReturns,
            excluded = true,
            finalReturnsCompleted = false,
            iossNumber = iossNumber,
            completeOrExcludedReturns = completeOfExcludedReturns
          ))
        }
      }
    }

    "must respond with Unauthorized when the user is not authorised" in {

      val app =
        new GuiceApplicationBuilder()
          .overrides(bind[AuthConnector].toInstance(new FakeFailingAuthConnector(new MissingBearerToken)))
          .build()

      running(app) {
        val result = route(app, request).value
        status(result) mustEqual UNAUTHORIZED
      }
    }
  }


}
