package uk.gov.hmrc.iossreturns.controllers.actions

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData
import uk.gov.hmrc.iossreturns.testUtils.RegistrationData.etmpRegistration

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class FakeAuthAction @Inject()(bodyParsers: BodyParsers.Default)
  extends AuthActionImpl(mock[AuthConnector], bodyParsers, mock[AppConfig], mock[RegistrationConnector]) {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    block(AuthorisedRequest(request, "id", Vrn("123456789"), "IM9001234567", RegistrationData.registrationWrapper))
}
