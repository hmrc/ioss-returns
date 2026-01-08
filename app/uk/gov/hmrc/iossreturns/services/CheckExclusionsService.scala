/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.iossreturns.config.Constants.excludedReturnAndPaymentExpiry
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusion
import uk.gov.hmrc.iossreturns.models.etmp.registration.EtmpExclusionReason.Reversal

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class CheckExclusionsService @Inject()(clock: Clock) {

  private def hasActiveWindowExpired(dueDate: LocalDate): Boolean = {
    val today = LocalDate.now(clock)
    today.isAfter(dueDate.plusYears(excludedReturnAndPaymentExpiry))
  }

  def getLastExclusionWithoutReversal(exclusions: List[EtmpExclusion]): Option[EtmpExclusion] = {
    // Even though API is array ETMP only return single item
    exclusions.headOption.filterNot(_.exclusionReason == Reversal)
  }

  def isPeriodExcluded(period: Period, exclusions: List[EtmpExclusion]): Boolean = {
    val excluded = getLastExclusionWithoutReversal(exclusions)

    excluded match {
      case Some(excluded) if excluded.exclusionReason != Reversal &&
        (excluded.effectiveDate.isBefore(period.firstDay) || excluded.effectiveDate == period.firstDay) => true
      case _ => false
    }
  }

  def isPeriodExpired(period: Period, exclusions: List[EtmpExclusion]): Boolean = {
    val excluded = getLastExclusionWithoutReversal(exclusions)

    excluded match {
      case Some(excluded) if excluded.exclusionReason != Reversal && hasActiveWindowExpired(period.paymentDeadline) => true
      case _ => false
    }
  }
}
