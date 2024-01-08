package uk.gov.hmrc.iossreturns.base

import org.scalatest.{OptionValues, TryValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossreturns.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models._

import java.time.{Clock, Instant, LocalDate, LocalDateTime, Month, ZoneId}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar
    with Generators {

  protected val vrn: Vrn = Vrn("123456789")

  val iossNumber = "IM9001234567"
  def period: Period = Period(2021, Month.NOVEMBER)

  val userAnswersId: String = "12345-credId"
  val testCredentials: Credentials = Credentials(userAnswersId, "GGW")

  val arbitraryInstant: Instant = arbitraryDate.arbitrary.sample.value.atStartOfDay(ZoneId.systemDefault).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault)

  val coreVatReturn: CoreVatReturn = CoreVatReturn(
    vatReturnReferenceNumber = "XI/XI063407423/M11.2086",
    version = Instant.ofEpochSecond(1630670836),
    traderId = CoreTraderId(vrn.vrn, "XI"),
    period = CorePeriod(2021, "03"),
    startDate = LocalDate.now(stubClockAtArbitraryDate),
    endDate = LocalDate.now(stubClockAtArbitraryDate),
    submissionDateTime = Instant.now(stubClockAtArbitraryDate),
    totalAmountVatDueGBP = BigDecimal(10),
    msconSupplies = List(CoreMsconSupply(
      "DE",
      BigDecimal(10),
      BigDecimal(10),
      BigDecimal(10),
      BigDecimal(-10),
      List(CoreSupply(
        "GOODS",
        BigDecimal(10),
        "STANDARD",
        BigDecimal(10),
        BigDecimal(10)
      )),
      List(CoreMsestSupply(
        Some("FR"),
        None,
        List(CoreSupply(
          "GOODS",
          BigDecimal(10),
          "STANDARD",
          BigDecimal(10),
          BigDecimal(10)
        ))
      )),
      List(CoreCorrection(
        CorePeriod(2021, "02"),
        BigDecimal(-10)
      ))
    )),
    changeDate = Some(LocalDateTime.now(stubClockAtArbitraryDate))
  )

  protected def applicationBuilder(clock: Option[Clock] = None): GuiceApplicationBuilder = {
    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[Clock].toInstance(clockToBind),
        bind[AuthAction].to[FakeAuthAction]
      )
  }
}
