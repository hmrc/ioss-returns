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
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.controllers.actions.TestAuthRetrievals._
import uk.gov.hmrc.iossreturns.models.RegistrationWrapper
import uk.gov.hmrc.iossreturns.services.AccountService
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with BeforeAndAfterEach {

  private type RetrievalsType = Option[Credentials] ~ Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ ConfidenceLevel

  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatAndIossEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"), Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")))
  private val iossEnrolment = Enrolments(Set(Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")))

  private val registrationWrapper: RegistrationWrapper = mock[RegistrationWrapper]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService: AccountService = mock[AccountService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockAccountService)
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
              .thenReturn(Future.successful(Some(testCredentials) ~ Some("id") ~ vatAndIossEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture
            when(mockAccountService.getLatestAccount(any())(any())) thenReturn iossNumber.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
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
              .thenReturn(Future.successful(Some(testCredentials) ~ Some("id") ~ Enrolments(Set.empty) ~ Some(Organisation) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
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
              .thenReturn(Future.successful(Some(testCredentials) ~ Some("id") ~ iossEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
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
              .thenReturn(Future.successful(Some(testCredentials) ~ Some("id") ~ vatEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
            verifyNoInteractions(mockRegistrationConnector)
          }
        }
      }

      "when the user is logged in as an individual without CL >=250" - {

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
              .thenReturn(Future.successful(Some(testCredentials) ~ Some("id") ~ vatAndIossEnrolment ~ Some(Individual) ~ ConfidenceLevel.L250))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual OK
            verify(mockRegistrationConnector, times(1)).getRegistration()(any())
          }
        }

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
              .thenReturn(Future.successful(Some(testCredentials) ~ Some("id") ~ vatAndIossEnrolment ~ Some(Individual) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
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

            when(mockRegistrationConnector.getRegistration()(any())) thenReturn registrationWrapper.toFuture

            val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new MissingBearerToken), bodyParsers, application.injector.instanceOf[AppConfig], mockRegistrationConnector, mockAccountService)
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
