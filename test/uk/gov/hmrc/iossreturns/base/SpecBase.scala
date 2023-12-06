package base

import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.TryValues
import play.api.inject.guice.GuiceApplicationBuilder

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar {

  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
}
