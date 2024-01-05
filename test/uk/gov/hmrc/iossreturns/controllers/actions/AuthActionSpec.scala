package uk.gov.hmrc.iossreturns.controllers.actions

import com.google.inject.Inject
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.controllers.actions.TestAuthRetrievals._
import uk.gov.hmrc.iossreturns.models.EtmpRegistration
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with BeforeAndAfterEach {

  private type RetrievalsType = Option[String] ~ Enrolments

  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatAndIossEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"), Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")))
  private val iossEnrolment = Enrolments(Set(Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")))

  private val etmpRegistration: EtmpRegistration = arbitraryEtmpRegistration.arbitrary.sample.value
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockRegistrationConnector)
  }

  class Harness(authAction: AuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth action" - {

    "when enrolments is enabled" - {
      "when the user is logged in and has a VAT enrolment and OSS enrolment" - {

        "must succeed and retrieve an ETMP registration" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            )
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ vatAndIossEnrolment))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn RegistrationData.registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector)
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual OK
            verify(mockRegistrationConnector, times(1)).getRegistration()(any())
          }
        }
      }

      "when the user is logged in without a VAT enrolment nor OSS enrolment" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            )
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ Enrolments(Set.empty)))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn RegistrationData.registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector)
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
            verifyNoInteractions(mockRegistrationConnector)
          }
        }
      }

      "when the user is logged in without a VAT enrolment with OSS enrolment" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            )
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ iossEnrolment))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn RegistrationData.registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector)
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
            verifyNoInteractions(mockRegistrationConnector)
          }
        }
      }

      "when the user is logged in without a OSS enrolment with VAT enrolment" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            )
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ vatEnrolment))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn RegistrationData.registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector)
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
            verifyNoInteractions(mockRegistrationConnector)
          }
        }
      }

      "when the user is not logged in" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            )
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockRegistrationConnector.getRegistration()(any())) thenReturn RegistrationData.registrationWrapper.toFuture

            val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new MissingBearerToken), bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector)
            val controller = new Harness(authAction)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustBe UNAUTHORIZED
            verifyNoInteractions(mockRegistrationConnector)
          }
        }
      }
    }

  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
