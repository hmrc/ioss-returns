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

import uk.gov.hmrc.iossreturns.models.requests.SaveForLaterRequest
import uk.gov.hmrc.iossreturns.models.{Period, SavedUserAnswers}
import uk.gov.hmrc.iossreturns.repository.SaveForLaterRepository

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterService @Inject()(
                                     repository: SaveForLaterRepository,
                                     clock: Clock
                                   )(implicit ec: ExecutionContext) {

  def saveAnswers(request: SaveForLaterRequest): Future[SavedUserAnswers] = {
    val answers = SavedUserAnswers(
      iossNumber = request.iossNumber,
      period = request.period,
      data = request.data,
      lastUpdated = Instant.now(clock)
    )
    repository.set(answers)
  }

  def get(iossNumber: String): Future[Seq[SavedUserAnswers]] =
    repository.get(iossNumber)

  def get(iossNumbers: Seq[String]): Future[Seq[SavedUserAnswers]] = {
    repository.get(iossNumbers)
      .map(_.sortBy(_.lastUpdated))
  }

  def delete(iossNumber: String, period: Period): Future[Boolean] =
    repository.clear(iossNumber, period)
}
