package uk.gov.hmrc.iossreturns.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.{FinancialDataConnector, FinancialDataConnectorFixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FinancialDataServiceSpec extends SpecBase with ScalaCheckPropertyChecks
  with BeforeAndAfterEach
  with OptionValues
  with FinancialDataConnectorFixture
   {

  private val mockFinancialDataConnector = mock[FinancialDataConnector]

  private val financialDataService = new FinancialDataService(mockFinancialDataConnector)
  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataConnector)
    super.beforeEach()

  }
  "Financial Data Service" - {
    val financialData = expectedResult
    "return back the exact result retrieved from the connector" in {
      when(mockFinancialDataConnector.getFinancialData(any(), any())) thenReturn Future.successful(financialData)

      whenReady(financialDataService.getFinancialData(iossNumber, None, None), PatienceConfiguration.Timeout(Span(2, Seconds))) { maybeFinancialData =>
        Right(maybeFinancialData) mustEqual financialData
      }

    }
  }
}
