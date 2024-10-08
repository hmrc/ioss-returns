package uk.gov.hmrc.iossreturns.services

import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.{FinancialDataConnector, FinancialDataConnectorFixture}
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialDataQueryParameters, FinancialTransaction, Item}
import uk.gov.hmrc.iossreturns.models.payments.Charge
import uk.gov.hmrc.iossreturns.models.{PeriodYear, StandardPeriod}
import uk.gov.hmrc.iossreturns.testUtils.FinancialTransactionData.getFinancialData
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import java.time.{LocalDate, Month, ZonedDateTime}
import scala.concurrent.ExecutionContext.Implicits.global

class FinancialDataServiceSpec extends SpecBase with ScalaCheckPropertyChecks
  with BeforeAndAfterEach
  with OptionValues
  with FinancialDataConnectorFixture {

  private val mockFinancialDataConnector = mock[FinancialDataConnector]
  private val mockReturnService = mock[ReturnsService]

  private val financialDataService = new FinancialDataService(mockFinancialDataConnector, mockReturnService)
  private val periodYear2021 = PeriodYear(2021)
  private val queryParameters2021 =
    FinancialDataQueryParameters(fromDate = Some(periodYear2021.startOfYear), toDate = Some(periodYear2021.endOfYear))

  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataConnector)
    super.beforeEach()

  }

  "Financial Data Service" - {

    ".getFinancialData" - {

      val items = Seq(
        Item(
          amount = None,
          clearingReason = None,
          paymentReference = None,
          paymentAmount = None,
          paymentMethod = None
        )
      )

      "must return Some(FinancialData) for 1 period year" in {
        val commencementDate = LocalDate.of(2021, 9, 1)
        val toDate = LocalDate.of(2021, 12, 31)

        val financialData =
          FinancialData(Some("IOSS"), Some("123456789"), Some("IOSS"), ZonedDateTime.now(), Option(financialTransactions))

        when(mockReturnService.getPeriodYears(any(), any())) thenReturn Seq(periodYear2021)
        when(mockFinancialDataConnector.getFinancialData(any(), equalTo(queryParameters2021))) thenReturn Some(financialData).toFuture

        financialDataService.getFinancialData(iossNumber, Some(commencementDate), Some(toDate))
      }

      "must return Some(FinancialData) for 2 period years" in {
        val commencementDate = LocalDate.of(2021, 9, 1)
        val toDate = LocalDate.of(2021, 12, 31)
        val periodYear2 = PeriodYear(2022)
        val queryParameters2 = FinancialDataQueryParameters(fromDate = Some(periodYear2.startOfYear), toDate = Some(periodYear2.endOfYear))

        val financialTransaction2 = Seq(
          FinancialTransaction(
            chargeType = None,
            mainType = None,
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            originalAmount = None,
            outstandingAmount = None,
            clearedAmount = None,
            items = Some(items)
          )
        )

        val financialData =
          FinancialData(Some("IOSS"), Some("123456789"), Some("IOSS"), ZonedDateTime.now(), Option(financialTransactions))

        val financialData2 =
          FinancialData(Some("IOSS"), Some("123456789"), Some("IOSS"), ZonedDateTime.now(), Option(financialTransaction2))

        when(mockReturnService.getPeriodYears(any(), any())) thenReturn Seq(periodYear2021, periodYear2)
        when(mockFinancialDataConnector.getFinancialData(any(), any()))
          .thenReturn(Some(financialData).toFuture)
          .thenReturn(Some(financialData2).toFuture)

        val response = financialDataService.getFinancialData(iossNumber, Some(commencementDate), Some(toDate)).futureValue

        response mustBe Some(
          financialData.copy(
            financialTransactions = Some(financialTransactions ++ financialTransaction2)
          )
        )
        verify(mockFinancialDataConnector, times(1))
          .getFinancialData(any(), eqTo(queryParameters2021))
        verify(mockFinancialDataConnector, times(1))
          .getFinancialData(any(), eqTo(queryParameters2))
      }

      "must return None when no financialData" in {
        val commencementDate = LocalDate.of(2021, 9, 1)
        val toDate = LocalDate.of(2021, 12, 31)

        when(mockReturnService.getPeriodYears(any(), any())) thenReturn Seq(periodYear2021)
        when(mockFinancialDataConnector.getFinancialData(any(), equalTo(queryParameters2021))) thenReturn None.toFuture

        financialDataService.getFinancialData(iossNumber, Some(commencementDate), Some(toDate)).futureValue mustBe None
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

            when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Some(financialData).toFuture

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

            when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Some(financialData).toFuture

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

            when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn Some(financialData).toFuture

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

          when(mockFinancialDataConnector.getFinancialData(any(), eqTo(queryParameters))) thenReturn None.toFuture

          val response = financialDataService.getCharge(iossNumber, period).futureValue

          response.isDefined mustBe false
          response mustBe None
        }
      }
    }
  }
}
