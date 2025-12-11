/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, anyString, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.requests.SaveForLaterRequest
import uk.gov.hmrc.iossreturns.models.{Period, SavedUserAnswers}
import uk.gov.hmrc.iossreturns.repository.SaveForLaterRepository
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SaveForLaterServiceSpec
  extends SpecBase
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach {

  private val mockSaveForLaterRepository: SaveForLaterRepository = mock[SaveForLaterRepository]

  private val now: Instant = Instant.now
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault())

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterRepository)
  }

  ".saveAnswers" - {

    "must create a SavedUserAnswers, attempt to save it to the repository, and respond with the result of saving" in {

      val answers = arbitrary[SavedUserAnswers].sample.value
      val insertResult = answers

      when(mockSaveForLaterRepository.set(any())) thenReturn insertResult.toFuture

      val request = arbitrary[SaveForLaterRequest].sample.value
      val service = new SaveForLaterService(mockSaveForLaterRepository, stubClock)

      val result = service.saveAnswers(request).futureValue

      result `mustBe` insertResult
      verify(mockSaveForLaterRepository, times(1)).set(any())
    }
  }

  ".get" - {

    "must retrieve a sequence of Saved User Answers record" in {

      val answers = arbitrary[SavedUserAnswers].sample.value
      val iossNumber = arbitrary[String].sample.value

      when(mockSaveForLaterRepository.get(anyString())) thenReturn Seq(answers).toFuture
      val service = new SaveForLaterService(mockSaveForLaterRepository, stubClock)

      val result = service.get(iossNumber).futureValue
      result `mustBe` Seq(answers)
      verify(mockSaveForLaterRepository, times(1)).get(iossNumber)
    }
  }

  ".get (Multiple)" - {

    "must retrieve a sequence of Saved User Answers records" in {

      val answers = Gen.listOfN(3, arbitrary[SavedUserAnswers]).sample.value
      val iossNumbers: Seq[String] = Gen.listOfN(3, arbitrary[String]).sample.value

      when(mockSaveForLaterRepository.get(eqTo(iossNumbers))) thenReturn answers.toFuture
      val service = new SaveForLaterService(mockSaveForLaterRepository, stubClock)

      val result = service.get(iossNumbers).futureValue
      result `mustBe` answers
      verify(mockSaveForLaterRepository, times(1)).get(iossNumbers)
    }
  }

  ".delete" - {

    "must delete a single Saved User Answers record" in {

      val iossNumber = arbitrary[String].sample.value
      val period = arbitrary[Period].sample.value

      when(mockSaveForLaterRepository.clear(any(), any())) thenReturn true.toFuture
      val service = new SaveForLaterService(mockSaveForLaterRepository, stubClock)

      val result = service.delete(iossNumber, period).futureValue
      result `mustBe` true
      verify(mockSaveForLaterRepository, times(1)).clear(iossNumber, period)
    }
  }
}
