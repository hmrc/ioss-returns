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

package uk.gov.hmrc.iossreturns.models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models.Period.getNext

import java.time.{LocalDate, Month}

class PeriodSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with EitherValues {

  private val pathBindable = implicitly[PathBindable[Period]]
  private val queryBindable = implicitly[QueryStringBindable[Period]]

  "Period" - {
    "pathBindable" - {
      "must bind from a URL" in {

        forAll(arbitrary[Period]) {
          period =>
            pathBindable.bind("key", period.toString).value mustEqual period
        }
      }

      "must not bind from an invalid value" in {

        pathBindable.bind("key", "invalid").left.value mustEqual "Invalid period"
      }
    }

    "queryBindable" - {
      "must bind from a query parameter when valid period present" in {

        forAll(arbitrary[Period]) {
          period =>

            queryBindable.bind("key", Map("key" -> Seq(period.toString))) mustBe Some(Right(period))
        }
      }

      "must not bind from an invalid value" in {

        queryBindable.bind("key", Map("key" -> Seq("invalid"))) mustBe Some(Left("Invalid period"))
      }

      "must return none if no query parameter present" in {
         queryBindable.bind("key", Map("key" -> Seq.empty)) mustBe None
      }
    }

    "getNext" - {
      "when current period is January" in {
        val year = 2021
        val current = StandardPeriod(year, Month.JANUARY)
        val expected = StandardPeriod(year, Month.FEBRUARY)

        getNext(current) mustBe expected
      }

      "when current period is February" in {
        val year = 2021
        val current = StandardPeriod(year, Month.FEBRUARY)
        val expected = StandardPeriod(year, Month.MARCH)

        getNext(current) mustBe expected
      }

      "when current period is July" in {
        val year = 2021
        val current = StandardPeriod(year, Month.JULY)
        val expected = StandardPeriod(year, Month.AUGUST)

        getNext(current) mustBe expected
      }

      "when current month is December" in {
        val year = 2021
        val current = StandardPeriod(year, Month.DECEMBER)
        val expected = StandardPeriod(year + 1, Month.JANUARY)

        getNext(current) mustBe expected
      }
    }
  }

  ".firstDay" - {

    "must be the first of the month when the month is January" in {

      forAll(Gen.choose(2023, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.JANUARY)
          period.firstDay mustEqual LocalDate.of(year, Month.JANUARY, 1)
      }
    }
  }

  ".lastDay" - {

    "must be the last of the month when the month is January" in {

      forAll(Gen.choose(2023, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.JANUARY)
          period.lastDay mustEqual LocalDate.of(year, Month.JANUARY, 31)
      }
    }
  }

  ".isBefore" - {

    "should return true when current period is before other period" in {
      val currentPeriod = StandardPeriod(2022, Month.JANUARY)
      val otherPeriod = StandardPeriod(2022, Month.FEBRUARY)

      val result = currentPeriod.isBefore(otherPeriod)

      assert(result)
    }

    "should return false when current period is before other period" in {
      val currentPeriod = StandardPeriod(2022, Month.JANUARY)
      val otherPeriod = StandardPeriod(2022, Month.FEBRUARY)

      val result = otherPeriod.isBefore(currentPeriod)

      assert(!result)
    }
  }

}
