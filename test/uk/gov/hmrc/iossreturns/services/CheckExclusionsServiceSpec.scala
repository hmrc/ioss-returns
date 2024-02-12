package uk.gov.hmrc.iossreturns.services

import uk.gov.hmrc.iossreturns.base.SpecBase
import java.time.LocalDate

class CheckExclusionsServiceSpec extends SpecBase {

  "CheckExclusionsService" - {

    ".hasActiveReturnWindowExpired" - {

      val service = new CheckExclusionsService(stubClockAtArbitraryDate)

      "must return true if active return window has expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3).minusDays(1)

        val result = service.hasActiveWindowExpired(dueDate)

        result mustBe true
      }

      "must return false if active return window is on the day of expiry" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3)
        val result = service.hasActiveWindowExpired(dueDate)

        result mustBe false
      }

      "must return false if active return window has not expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2)
        val result = service.hasActiveWindowExpired(dueDate)

        result mustBe false
      }
    }
  }
}
