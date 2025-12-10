package uk.gov.hmrc.iossreturns.controllers.actions

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.services.AccountService
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData.etmpRegistration

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class FakeAuthAction(bodyParsers: BodyParsers.Default)
  extends AuthAction(
    mock[AuthConnector],
    bodyParsers,
    mock[AppConfig],
    mock[RegistrationConnector],
    mock[AccountService],
    None
  ) {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    block(AuthorisedRequest(request, "id", "credId", Vrn("123456789"), "IM9001234567", etmpRegistration, None))
}
class FakeAuthActionProvider @Inject()(bodyParsers: BodyParsers.Default)
  extends AuthActionProvider(
    mock[AuthConnector],
    bodyParsers,
    mock[AppConfig],
    mock[RegistrationConnector],
    mock[AccountService]
  )(ExecutionContext.Implicits.global) {

  override def apply(maybeIossNumber: Option[String] = None): AuthAction = new FakeAuthAction(bodyParsers)

}
