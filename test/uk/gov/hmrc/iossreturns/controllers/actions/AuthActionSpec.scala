package uk.gov.hmrc.iossreturns.controllers.actions

import com.google.inject.Inject
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{~, Retrieval}
import TestAuthRetrievals._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.config.AppConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with BeforeAndAfterEach {

  private type RetrievalsType = Option[String] ~ Enrolments

  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatAndIossEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"), Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")))
  private val iossEnrolment = Enrolments(Set(Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")))

  class Harness(authAction: AuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
  }

  "Auth action" - {

    "when enrolments is enabled" - {
      "when the user is logged in and has a VAT enrolment and OSS enrolment" - {

        "must succeed" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            )
            .build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ vatAndIossEnrolment))

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig])
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual OK
          }
        }
      }

      "when the user is logged in without a VAT enrolment nor OSS enrolment" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            ).build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ Enrolments(Set.empty)))

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig])
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
          }
        }
      }

      "when the user is logged in without a VAT enrolment with OSS enrolment" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            ).build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ iossEnrolment))

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig])
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
          }
        }
      }

      "when the user is logged in without a OSS enrolment with VAT enrolment" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            ).build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some("id") ~ vatEnrolment))

            val action = new AuthActionImpl(mockAuthConnector, bodyParsers, application.injector.instanceOf[AppConfig])
            val controller = new Harness(action)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustEqual UNAUTHORIZED
          }
        }
      }

      "when the user is not logged in" - {

        "must return Unauthorized" in {

          val application = applicationBuilder()
            .configure(
              "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
            ).build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

            val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new MissingBearerToken), bodyParsers, application.injector.instanceOf[AppConfig])
            val controller = new Harness(authAction)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustBe UNAUTHORIZED
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
