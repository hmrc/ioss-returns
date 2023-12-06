package uk.gov.hmrc.iossreturns.services

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.iossreturns.connectors.{FinancialDataConnector, FinancialDataConnectorFixture}
import uk.gov.hmrc.iossreturns.models.IOSSNumber

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FinancialDataServiceSpec extends SpecBase with ScalaCheckPropertyChecks
  with BeforeAndAfterEach
  with OptionValues {

  private val mockFinancialDataConnector = mock[FinancialDataConnector]

  private val financialDataService = new FinancialDataService(mockFinancialDataConnector)
  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataConnector)
    super.beforeEach()

  }
  "Financial Data Service" - {
    val financialData = FinancialDataConnectorFixture.expectedResult
    "return back the exact result retrieved from the connector" in {
      when(mockFinancialDataConnector.getFinancialData(IOSSNumber(anyString()), any())) thenReturn Future.successful(financialData)

      whenReady(financialDataService.getFinancialData(FinancialDataConnectorFixture.iossNumber, None, None), PatienceConfiguration.Timeout(Span(2, Seconds))) { maybeFinancialData =>
        Right(maybeFinancialData) mustEqual financialData
      }


    }
  }
}
