package uk.gov.hmrc.iossreturns.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers, PreviousRegistration}

import java.time.{LocalDateTime, YearMonth}
import scala.concurrent.Future

class PreviousRegistrationServiceSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  "PreviousRegistrationService" - {
    "correctly generate a list of PreviousRegistrations from enrolments" in {

      val mockRegistrationConnector = mock[RegistrationConnector]
      val service = new PreviousRegistrationService(mockRegistrationConnector)

      val credId = "credId"
      val customEnrolments = EACDEnrolments(Seq(
        EACDEnrolment(
          service = "TestService",
          state = "Activated",
          activationDate = Some(LocalDateTime.of(2023, 1, 1, 0, 0)),
          identifiers = Seq(EACDIdentifiers(key = "IOSSNumber", value = "IOSSTest1"))
        ),
        EACDEnrolment(
          service = "TestService",
          state = "Activated",
          activationDate = Some(LocalDateTime.of(2023, 5, 1, 0, 0)),
          identifiers = Seq(EACDIdentifiers(key = "IOSSNumber", value = "IOSSTest2"))
        )
      ))

      when(mockRegistrationConnector.getAccounts(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(customEnrolments))

      val expectedPreviousRegistrations = List(
        PreviousRegistration(
          startPeriod = Period(YearMonth.of(2023, 1)),
          endPeriod = Period(YearMonth.of(2023, 4)),
          iossNumber = "IOSSTest1"
        )
      )

      val result = service.getPreviousRegistrations(credId).futureValue

      result mustEqual expectedPreviousRegistrations
    }

    "return an empty list if no valid enrolments are found" in {

      val mockRegistrationConnector = mock[RegistrationConnector]

      val service = new PreviousRegistrationService(mockRegistrationConnector)

      val credId = "credId"
      val eACDEnrolments = arbitraryEACDEnrolments.arbitrary.sample.value

      when(mockRegistrationConnector.getAccounts(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(eACDEnrolments))

      val result = service.getPreviousRegistrations(credId).futureValue

      result mustBe empty
    }
  }

}
