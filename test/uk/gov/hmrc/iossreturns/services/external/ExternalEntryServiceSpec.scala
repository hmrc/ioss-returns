package uk.gov.hmrc.iossreturns.services.external

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.external.*
import uk.gov.hmrc.iossreturns.repository.ExternalEntryRepository
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExternalEntryServiceSpec extends SpecBase with BeforeAndAfterEach {
  val userId = "1234"
  val externalRequest = ExternalRequest("BTA", "/bta")
  val currentPeriod: Period = arbitraryPeriod.arbitrary.sample.value

  private val mockExternalEntryRepository: ExternalEntryRepository = mock[ExternalEntryRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockExternalEntryRepository)
  }

  ".getExternalResponse" - {

    "when entry page in the request is Your Account" - {

      "and no period is provided" - {
        "and language specified is Welsh" - {
          "must return correct response" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, YourAccount.name, None, Some("cy"))

            result mustBe Right(
              ExternalResponse(
                NoMoreWelsh.url(YourAccount.url)
              )
            )
            verify(mockExternalEntryRepository, times(1)).set(externalEntry)
          }
        }

        "and language is not Welsh" - {
          "must return correct response" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, YourAccount.name, None, None)

            result mustBe Right(
              ExternalResponse(
                YourAccount.url
              )
            )
            verify(mockExternalEntryRepository, times(1)).set(externalEntry)
          }
        }

        "and period is provided" - {
          "must return Left(NotFound) and not save url in session" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, YourAccount.name, Some(period), None)

            result mustBe Left(ErrorResponse(500, s"Unknown external entry ${YourAccount.name}"))
            verifyNoInteractions(mockExternalEntryRepository)
          }
        }
      }
    }

    "when entry page in the request is Returns History" - {

      "and no period is provided" - {
        "and language specified is Welsh" - {
          "must return correct response" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, ReturnsHistory.name, None, Some("cy"))

            result mustBe Right(
              ExternalResponse(
                NoMoreWelsh.url(ReturnsHistory.url(iossNumber))
              )
            )
            verify(mockExternalEntryRepository, times(1)).set(externalEntry)
          }
        }

        "and language is not Welsh" - {
          "must return correct response" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, ReturnsHistory.name, None, None)

            result mustBe Right(
              ExternalResponse(
                ReturnsHistory.url(iossNumber)
              )
            )
            verify(mockExternalEntryRepository, times(1)).set(externalEntry)
          }
        }

        "and period is provided" - {
          "must return Left(NotFound) and not save url in session" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, ReturnsHistory.name, Some(period), None)

            result mustBe Left(ErrorResponse(500, s"Unknown external entry ${ReturnsHistory.name}"))
            verifyNoInteractions(mockExternalEntryRepository)
          }
        }
      }
    }

    Seq(StartReturn, ContinueReturn).foreach {
      entryPage =>
        s"when entry page in the request is ${entryPage}" - {

          "and period is provided" - {
            "and language specified is Welsh" - {
              "must return correct response" in {

                val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
                val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

                when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
                val result = service.getExternalResponse(externalRequest, userId, iossNumber, entryPage.name, Some(currentPeriod), Some("cy"))

                result mustBe Right(
                  ExternalResponse(
                    NoMoreWelsh.url(entryPage.url(iossNumber, currentPeriod))
                  )
                )

                verify(mockExternalEntryRepository, times(1)).set(externalEntry)
              }
            }

            "and language is not Welsh" - {
              "must return correct response" in {

                val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
                val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

                when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
                val result = service.getExternalResponse(externalRequest, userId, iossNumber, entryPage.name, Some(currentPeriod), None)

                result mustBe Right(
                  ExternalResponse(
                    entryPage.url(iossNumber, currentPeriod)
                  )
                )
                verify(mockExternalEntryRepository, times(1)).set(externalEntry)
              }
            }
          }

          "and period is not provided" - {
            "must return Left(NotFound) and not save url in session" in {

              val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
              val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

              when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
              val result = service.getExternalResponse(externalRequest, userId, iossNumber, entryPage.name, None, None)

              result mustBe Left(ErrorResponse(500, s"Unknown external entry ${entryPage.name}"))
              verifyNoInteractions(mockExternalEntryRepository)
            }
          }
        }
    }

    "must return an External Response when sessionRepository fails due to exception" in {

      val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)

      when(mockExternalEntryRepository.set(any())) thenReturn Future.failed(new Exception("Error saving in session"))
      val result = service.getExternalResponse(externalRequest, userId, iossNumber, YourAccount.name, None, None)

      result mustBe Right(
        ExternalResponse(
          YourAccount.url
        )
      )
    }

    "when entry page in the request is Payment" - {

      "and no period is provided" - {
        "and language is Welsh" - {
          "must return correct response" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, Payment.name, None, Some("cy"))

            result mustBe Right(
              ExternalResponse(
                NoMoreWelsh.url(Payment.url(iossNumber))
              )
            )
            verify(mockExternalEntryRepository, times(1)).set(externalEntry)
          }
        }

        "and language is not Welsh" - {
          "must return correct response" in {

            val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)
            val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(stubClockAtArbitraryDate))

            when(mockExternalEntryRepository.set(any())) thenReturn Future.successful(externalEntry)
            val result = service.getExternalResponse(externalRequest, userId, iossNumber, Payment.name, None, None)

            result mustBe Right(
              ExternalResponse(
                Payment.url(iossNumber)
              )
            )
            verify(mockExternalEntryRepository, times(1)).set(externalEntry)
          }
        }
      }
    }

  }

  ".getSavedResponseUrl" - {
    "must return the saved return URL" in {

      val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)

      val savedUrl = Some("/saved/return/url")
      when(mockExternalEntryRepository.get(userId)) thenReturn Future.successful(Some(ExternalEntry(userId, savedUrl.get, Instant.now(stubClockAtArbitraryDate))))

      val result = service.getSavedResponseUrl(userId).futureValue

      result mustBe savedUrl
      verify(mockExternalEntryRepository, times(1)).get(userId)
    }

    "must return None when no saved URL is found" in {

      val service = new ExternalEntryService(mockExternalEntryRepository, stubClockAtArbitraryDate)

      when(mockExternalEntryRepository.get(userId)) thenReturn Future.successful(None)

      val result = service.getSavedResponseUrl(userId).futureValue

      result mustBe None
      verify(mockExternalEntryRepository, times(1)).get(userId)
    }
  }
}
