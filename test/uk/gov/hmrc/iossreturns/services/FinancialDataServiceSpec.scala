package uk.gov.hmrc.iossreturns.services

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.{FinancialDataConnector, FinancialDataConnectorFixture}
import uk.gov.hmrc.iossreturns.models.StandardPeriod
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataQueryParameters}
import uk.gov.hmrc.iossreturns.models.payments.Charge
import uk.gov.hmrc.iossreturns.testUtils.FinancialTransactionData.getFinancialData
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import java.time.Month
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FinancialDataServiceSpec extends SpecBase with ScalaCheckPropertyChecks
  with BeforeAndAfterEach
  with OptionValues
  with FinancialDataConnectorFixture {

  private val mockFinancialDataConnector = mock[FinancialDataConnector]

  private val financialDataService = new FinancialDataService(mockFinancialDataConnector)

  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataConnector)
    super.beforeEach()

  }

  "Financial Data Service" - {

    ".getFinancialData" - {

      val financialData = expectedResult
      "return back the exact result retrieved from the connector" in {
        when(mockFinancialDataConnector.getFinancialData(any(), any())) thenReturn Future.successful(financialData)

        whenReady(financialDataService.getFinancialData(iossNumber, None, None), PatienceConfiguration.Timeout(Span(2, Seconds))) { maybeFinancialData =>
          Right(maybeFinancialData) mustEqual financialData
        }
      }
    }

    ".getCharge" - {

      "must return a charge" - {

        "when there is financial data" - {

          "and there are no payments made" in {

            val financialData: FinancialData = getFinancialData(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(1000),
              clearedAmount = BigDecimal(0),
              numberOfTransactions = 1
            )

            val queryParameters = FinancialDataQueryParameters(fromDate = Some(period.firstDay), toDate = Some(period.lastDay))

            when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Right(Some(financialData)).toFuture

            val response = financialDataService.getCharge(iossNumber, period).futureValue

            val charge: Charge = response.get

            response.isDefined mustBe true
            response mustBe Some(charge)
          }

          "and there has been a payment and a single transaction" in {

            val financialData: FinancialData = getFinancialData(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(500),
              clearedAmount = BigDecimal(500),
              numberOfTransactions = 1
            )

            val queryParameters = FinancialDataQueryParameters(fromDate = Some(period.firstDay), toDate = Some(period.lastDay))

            when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Right(Some(financialData)).toFuture

            val response = financialDataService.getCharge(iossNumber, period).futureValue

            val charge: Charge = response.get

            response.isDefined mustBe true
            charge.period mustBe period
            charge.clearedAmount mustBe BigDecimal(500.00)
            charge.originalAmount mustBe BigDecimal(1000.00)
            charge.outstandingAmount mustBe BigDecimal(500.00)
          }

          "and there has been two transactions and two payments" in {

            val financialData: FinancialData = getFinancialData(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(500),
              clearedAmount = BigDecimal(750),
              numberOfTransactions = 2
            )

            val queryParameters = FinancialDataQueryParameters(fromDate = Some(period.firstDay), toDate = Some(period.lastDay))

            when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Right(Some(financialData)).toFuture

            val response = financialDataService.getCharge(iossNumber, period).futureValue

            val charge: Charge = response.get

            response.isDefined mustBe true
            charge.period mustBe period
            charge.clearedAmount mustBe BigDecimal(1500.00)
            charge.originalAmount mustBe BigDecimal(2000.00)
            charge.outstandingAmount mustBe BigDecimal(1000.00)
          }
        }
      }

      "must not return a charge" - {

        "when there is no financial data" in {
          val period = StandardPeriod(2021, Month.NOVEMBER)

          val queryParameters = FinancialDataQueryParameters(fromDate = Some(period.firstDay), toDate = Some(period.lastDay))

          when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Right(None).toFuture

          val response = financialDataService.getCharge(iossNumber, period).futureValue

          response.isDefined mustBe false
          response mustBe None
        }
      }
    }
  }
}
