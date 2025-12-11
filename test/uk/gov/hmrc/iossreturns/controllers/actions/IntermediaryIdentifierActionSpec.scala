package uk.gov.hmrc.iossreturns.controllers.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, running, status}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossreturns.controllers.actions.TestAuthRetrievals.Ops
import uk.gov.hmrc.iossreturns.models.etmp.intermediary.IntermediaryRegistrationWrapper
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IntermediaryIdentifierActionSpec extends SpecBase with BeforeAndAfterEach {

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockIntermediaryRegistrationConnector: IntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]

  private type RetrievalsType = Option[String] ~ Enrolments

  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val intermediaryEnrolment = Enrolments(Set(Enrolment("HMRC-IOSS-INT", Seq(EnrolmentIdentifier("IntNumber", "IN9001234567")), "Activated")))
  private val vatAndIntermediaryEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"), Enrolment("HMRC-IOSS-INT", Seq(EnrolmentIdentifier("IntNumber", "IN9001234567")), "Activated")))

  private val intermediaryRegistrationWrapper: IntermediaryRegistrationWrapper = arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value

  class Harness(intermediaryIdentifierAction: IntermediaryIdentifierActionImpl) {
    def onPageLoad(): Action[AnyContent] = intermediaryIdentifierAction { _ => Results.Ok }
  }

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockAuthConnector,
      mockIntermediaryRegistrationConnector
    )
  }

  "IntermediaryIdentifierAction" - {

    "when the user is not logged in" - {

      "must return Unauthorized" in {

        val application = applicationBuilder().build()

        running(application) {

          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            new FakeFailingAuthConnector(new MissingBearerToken),
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) `mustBe` UNAUTHORIZED
          verifyNoInteractions(mockAuthConnector)
          verifyNoInteractions(mockIntermediaryRegistrationConnector)
        }
      }
    }

    "when the user attempts to log in but auth unable to retrieve authorisation data" - {

      "must throw an UnauthorizedException" in {

        val application = applicationBuilder()
          .overrides(
            bind[AuthConnector].toInstance(mockAuthConnector)
          )
          .build()

        running(application) {

          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(None ~ Enrolments(Set.empty)))

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            mockAuthConnector,
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest())

          whenReady(result.failed) { exp =>
            exp `mustBe` a[UnauthorizedException]
            exp.getMessage `mustBe` "Unable to retrieve authorisation data"
          }

          verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
          verifyNoInteractions(mockIntermediaryRegistrationConnector)
        }
      }
    }

    "when the user is logged in without a VAT enrolment nor Intermediary enrolment" - {

      "must return Unauthorized" in {

        val application = applicationBuilder()
          .overrides(
            bind[AuthConnector].toInstance(mockAuthConnector)
          )
          .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ Enrolments(Set.empty)))

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            mockAuthConnector,
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) `mustBe` UNAUTHORIZED
          verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
          verifyNoInteractions(mockIntermediaryRegistrationConnector)
        }
      }
    }

    "when the user is logged in with a VAT enrolment but without an Intermediary enrolment" - {

      "must return Unauthorized" in {

        val application = applicationBuilder()
          .overrides(
            bind[AuthConnector].toInstance(mockAuthConnector)
          )
          .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatEnrolment))

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            mockAuthConnector,
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) `mustBe` UNAUTHORIZED
          verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
          verifyNoInteractions(mockIntermediaryRegistrationConnector)
        }
      }
    }

    "when the user is logged in without a VAT enrolment but with an Intermediary enrolment" - {

      "must return Unauthorized" in {

        val application = applicationBuilder()
          .overrides(
            bind[AuthConnector].toInstance(mockAuthConnector)
          )
          .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ intermediaryEnrolment))

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            mockAuthConnector,
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) `mustBe` UNAUTHORIZED
          verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
          verifyNoInteractions(mockIntermediaryRegistrationConnector)
        }
      }
    }

    "when the user is logged in with a VAT enrolment and an Intermediary enrolment" - {

      "must return OK with the retrieval of an Intermediary Registration Wrapper" in {

        val application = applicationBuilder()
          .overrides(
            bind[AuthConnector].toInstance(mockAuthConnector),
            bind[IntermediaryRegistrationConnector].toInstance(mockIntermediaryRegistrationConnector)
          )
          .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatAndIntermediaryEnrolment))

          when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn intermediaryRegistrationWrapper.toFuture

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            mockAuthConnector,
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest("GET", "/example"))

          status(result) `mustBe` OK
          verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
          verify(mockIntermediaryRegistrationConnector, times(1)).get(any())(any())
        }
      }

      "must throw an Exception when the server fails to retrieve an Intermediary Registration Wrapper" in {

        val errorMessage: String = "There was an error retrieving Intermediary registration."

        val application = applicationBuilder()
          .overrides(
            bind[AuthConnector].toInstance(mockAuthConnector),
            bind[IntermediaryRegistrationConnector].toInstance(mockIntermediaryRegistrationConnector)
          )
          .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatAndIntermediaryEnrolment))

          when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn Future.failed(Exception(errorMessage))

          val intermediaryIdentifierAction = new IntermediaryIdentifierActionImpl(
            mockAuthConnector,
            bodyParsers,
            mockIntermediaryRegistrationConnector,
            appConfig
          )

          val controller = new Harness(intermediaryIdentifierAction)
          val result = controller.onPageLoad()(FakeRequest())

          whenReady(result.failed) { exp =>
            exp `mustBe` a[Exception]
            exp.getMessage `mustBe` errorMessage
          }

          verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
          verify(mockIntermediaryRegistrationConnector, times(1)).get(any())(any())
        }
      }
    }
  }
}
