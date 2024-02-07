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
import uk.gov.hmrc.iossreturns.controllers.actions.{AuthAction, FakeAuthAction, FakeFailingAuthConnector}
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.etmp.registration.{EtmpExclusion, EtmpExclusionReason}
import uk.gov.hmrc.iossreturns.models.youraccount.{CurrentReturns, PeriodWithStatus, Return}
import uk.gov.hmrc.iossreturns.models.youraccount.SubmissionStatus.{Complete, Due, Excluded, Next, Overdue}
import uk.gov.hmrc.iossreturns.services.ReturnsService

import java.time.{Clock, LocalDate, Month, ZoneId}
import scala.concurrent.Future

class ReturnStatusControllerSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with Generators {
  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].to[FakeAuthAction]
      )

  ".getCurrentReturnsForIossNumber()" - {
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

    lazy val request = FakeRequest(GET, routes.ReturnStatusController.getCurrentReturnsForIossNumber(iossNumber).url)
    "must respond with OK and the OpenReturns model" - {

      "with no returns in progress, due or overdue if there are no returns due yet" in {
        val mockReturnService = mock[ReturnsService]
        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(Seq.empty)
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(Seq(
          ), false, false, iossNumber))
        }
      }
      "with no returns in progress, due or overdue if all returns are complete" in {

        val mockReturnService = mock[ReturnsService]
        val lastPeriod = periods.takeRight(1).head
        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          periods.dropRight(1).map(PeriodWithStatus(_, Complete)).toList ::: List(PeriodWithStatus(lastPeriod, Next))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(Seq(Return.fromPeriod(lastPeriod, Next, false, true)), false, false, iossNumber))
        }
      }

      "with a return due but not in progress if there's one return due but no saved answers" in {

        val mockReturnService = mock[ReturnsService]

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          List(PeriodWithStatus(period2022SEPTEMBER, Due))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(Seq(Return.fromPeriod(period2022SEPTEMBER, Due, false, true)), false, false, iossNumber))
        }
      }


      "with some overdue returns but nothing in progress" in {
        val periods = Seq(period2021APRIL,
          period2021MAY,
          period2021JUNE)

        val mockReturnService = mock[ReturnsService]
        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(periods.map(PeriodWithStatus(_, Overdue)))
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        val returns = Return.fromPeriod(periods.head, Overdue, false, true) :: periods.tail.map(Return.fromPeriod(_, Overdue, false, false)).toList

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            returns, false, false, iossNumber))
        }
      }


      "with a return due and some returns overdue and nothing in progress" in {
        val (period1 :: periodsInBetween) = periods.dropRight(1)
        val lastPeriod = periods.takeRight(1).head

        val mockReturnService = mock[ReturnsService]

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          (period1 :: periodsInBetween).map(PeriodWithStatus(_, Overdue))
            :::
            List(PeriodWithStatus(lastPeriod, Due))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn None

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(
            CurrentReturns(
              Return.fromPeriod(period1, Overdue, inProgress = false, isOldest = true) ::
                periodsInBetween.map(Return.fromPeriod(_, Overdue, inProgress = false, isOldest = false)).toList
                :::
                List(Return.fromPeriod(lastPeriod, Due, inProgress = false, isOldest = false)),
              excluded = false,
              finalReturnsCompleted = false,
              iossNumber = iossNumber
            ))
        }

      }
      "with an excluded trader's final return due but not in progress and no saved answers" in {

        val mockReturnService = mock[ReturnsService]

        val exclusion: Option[EtmpExclusion] = arbitraryEtmpExclusion.arbitrary.sample.map(_.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = period2022AUGUST.firstDay))

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          List(PeriodWithStatus(period2022AUGUST, Overdue), PeriodWithStatus(period2022SEPTEMBER, Excluded))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn false
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn exclusion

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()


        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            Seq(
              Return.fromPeriod(period2022AUGUST, Overdue, false, true)
            ),
            excluded = true,
            finalReturnsCompleted = false,
            iossNumber = iossNumber
          ))
        }
      }

      "with an excluded trader's final return completed and can't start any more returns" in {
        val mockReturnService = mock[ReturnsService]

        val exclusion: Option[EtmpExclusion] = arbitraryEtmpExclusion.arbitrary.sample.map(_.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = period2022AUGUST.firstDay))

        when(mockReturnService.getStatuses(any(), any(), any())) thenReturn Future.successful(
          List(PeriodWithStatus(period2022AUGUST, Complete), PeriodWithStatus(period2022SEPTEMBER, Excluded))
        )
        when(mockReturnService.hasSubmittedFinalReturn(any(), any())) thenReturn true
        when(mockReturnService.getLastExclusionWithoutReversal(any())) thenReturn exclusion

        val app = applicationBuilder
          .overrides(bind[ReturnsService].toInstance(mockReturnService))
          .overrides(bind[Clock].toInstance(stubClock))
          .build()

        running(app) {
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(CurrentReturns(
            Seq.empty,
            excluded = true,
            finalReturnsCompleted = true,
            iossNumber = iossNumber
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
