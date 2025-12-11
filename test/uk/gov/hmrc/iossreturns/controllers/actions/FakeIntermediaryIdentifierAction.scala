package uk.gov.hmrc.iossreturns.controllers.actions

import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{BodyParsers, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models.etmp.intermediary.IntermediaryRegistrationWrapper
import uk.gov.hmrc.iossreturns.models.requests.IntermediaryIdentifierRequest

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FakeIntermediaryIdentifierAction @Inject()(
                                                  bodyParsers: BodyParsers.Default
                                                )
  extends IntermediaryIdentifierActionImpl(
    mock[AuthConnector],
    bodyParsers,
    mock[IntermediaryRegistrationConnector],
    mock[AppConfig]
  ) with OptionValues
    with Generators {

  private val intermediaryRegistrationWrapper: IntermediaryRegistrationWrapper =
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value

  override def invokeBlock[A](request: Request[A], block: IntermediaryIdentifierRequest[A] => Future[Result]): Future[Result] = {
    block(IntermediaryIdentifierRequest(request, "id", Vrn("123456789"), "IN9001234567", intermediaryRegistrationWrapper))
  }
}
