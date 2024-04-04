package uk.gov.hmrc.iossreturns.services

import org.scalatest.PrivateMethodTester
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.StandardPeriod
import uk.gov.hmrc.iossreturns.models.etmp.registration.{EtmpExclusion, EtmpExclusionReason}

import java.time.LocalDate

class CheckExclusionsServiceSpec extends SpecBase with PrivateMethodTester {

  private val stubbedNow: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

  "CheckExclusionsService" - {

    ".hasActiveReturnWindowExpired" - {

      val service = new CheckExclusionsService(stubClockAtArbitraryDate)

      "must return true if active return window has expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3).minusDays(1)

        val hasActiveWindowExpired = PrivateMethod[LocalDate](Symbol("hasActiveWindowExpired"))
        service invokePrivate hasActiveWindowExpired(dueDate) mustBe true

      }

      "must return false if active return window is on the day of expiry" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3)

        val hasActiveWindowExpired = PrivateMethod[LocalDate](Symbol("hasActiveWindowExpired"))
        service invokePrivate hasActiveWindowExpired(dueDate) mustBe false
      }

      "must return false if active return window has not expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2)

        val hasActiveWindowExpired = PrivateMethod[LocalDate](Symbol("hasActiveWindowExpired"))
        service invokePrivate hasActiveWindowExpired(dueDate) mustBe false
      }
    }

    ".isPeriodExcluded" - {

      "must return true when period is excluded because hasActiveWindowExpired returns true" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val pastDate = stubbedNow.minusYears(3).minusMonths(2)

        val period = StandardPeriod(pastDate.getYear, pastDate.getMonth)

        val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.minusMonths(4)))

        val result = service.isPeriodExcluded(period, exclusion)

        result mustBe true
      }

      "must return true when period is excluded because hasActiveWindowExpired returns false and the period falls after the exclusion effective date" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val period = StandardPeriod(stubbedNow.getYear, stubbedNow.getMonth)

        val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.minusMonths(1)))

        val result = service.isPeriodExcluded(period, exclusion)

        result mustBe true
      }

      "must return true when period is excluded because hasActiveWindowExpired returns false and the period falls on the exclusion effective date" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val period = StandardPeriod(stubbedNow.getYear, stubbedNow.getMonth)

        val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = period.firstDay))

        val result = service.isPeriodExcluded(period, exclusion)

        result mustBe true
      }

      "must return true when period is excluded because hasActiveWindowExpired returns true and the period falls after the exclusion effective date" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val pastDate = stubbedNow.minusYears(3).minusMonths(2)

        val period = StandardPeriod(pastDate.getYear, pastDate.getMonth)

        val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.minusMonths(1)))

        val result = service.isPeriodExcluded(period, exclusion)

        result mustBe true
      }

      "must return false when period is not excluded because hasActiveWindowExpired returns false and the period falls before the exclusion effective date" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val period = StandardPeriod(stubbedNow.getYear, stubbedNow.getMonth)
        val exclusion = List(arbitraryEtmpExclusion.arbitrary.sample.value.copy(exclusionReason = EtmpExclusionReason.FailsToComply, effectiveDate = stubbedNow.plusMonths(1)))

        val result = service.isPeriodExcluded(period, exclusion)

        result mustBe false
      }

      "must return false when there is no exclusion" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val period = StandardPeriod(stubbedNow.getYear, stubbedNow.getMonth)
        val exclusion = List.empty

        val result = service.isPeriodExcluded(period, exclusion)

        result mustBe false
      }
    }

    ".getLastExclusionWithoutReversal" - {

      "must return Some(exclusionNonReversal) when exclusions exist and head of list is not exclusion reason Reversal" in {

        val exclusionReversal: EtmpExclusion = EtmpExclusion(
          exclusionReason = EtmpExclusionReason.Reversal,
          effectiveDate = stubbedNow,
          decisionDate = stubbedNow.minusMonths(1),
          quarantine = false
        )

        val exclusionNonReversal: EtmpExclusion = EtmpExclusion(
          exclusionReason = EtmpExclusionReason.FailsToComply,
          effectiveDate = stubbedNow,
          decisionDate = stubbedNow.minusMonths(1),
          quarantine = false
        )

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val result = service.getLastExclusionWithoutReversal(List(exclusionNonReversal, exclusionReversal))

        result mustBe Some(exclusionNonReversal)
      }

      // Can only ever have one exclusion so this scenario should never happen
      "must return None when exclusions exist and head of list is exclusion reason Reversal" in {

        val exclusionReversal: EtmpExclusion = EtmpExclusion(
          exclusionReason = EtmpExclusionReason.Reversal,
          effectiveDate = stubbedNow,
          decisionDate = stubbedNow.minusMonths(1),
          quarantine = false
        )

        val exclusionNonReversal: EtmpExclusion = EtmpExclusion(
          exclusionReason = EtmpExclusionReason.FailsToComply,
          effectiveDate = stubbedNow,
          decisionDate = stubbedNow.minusMonths(1),
          quarantine = false
        )

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val result = service.getLastExclusionWithoutReversal(List(exclusionReversal, exclusionNonReversal))

        result mustBe None
      }

      "must return None when no exclusions exist" in {

        val service = new CheckExclusionsService(stubClockAtArbitraryDate)

        val result = service.getLastExclusionWithoutReversal(List.empty)

        result mustBe None
      }
    }
  }
}
