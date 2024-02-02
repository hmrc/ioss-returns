/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossreturns.services

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.{FinancialDataConnector, VatReturnConnector}
import uk.gov.hmrc.iossreturns.connectors.FinancialDataHttpParser.FinancialDataResponse
import uk.gov.hmrc.iossreturns.models.Period
import uk.gov.hmrc.iossreturns.models.etmp._
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}
import uk.gov.hmrc.iossreturns.models.payments.Payment

import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with PaymentsServiceSpecFixture with ScalaCheckPropertyChecks {
  implicit val hc = HeaderCarrier()
  val someCommencementDate = LocalDate.now().minusYears(3)
  "PaymentsService" - {

    "must return payments when there are due payments and overdue payments - from FinancialData, vatReturn to be ignored" in {

      val periodOverdue = Period(2021, Month.JANUARY)
      val periodDue = Period(2021, Month.APRIL)

      val transactionAmountDue1 = 250
      val transactionAmountDue2 = 750
      val transactionAmountOverdue1 = 150
      val transactionAmountOverdue2 = 350

      val ft1 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountDue1)))
      val ft2 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountDue2)))
      val ft3 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = None)
      val ft4 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountOverdue1)))
      val ft5 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountOverdue2)))
      val ft6 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue.firstDay), outstandingAmount = None)

      val inputFinancialData = financialData.copy(financialTransactions = Some(List(ft1, ft2, ft3, ft4, ft5, ft6)))

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]
      val periodOverdueKey = "21AA"
      val periodDueKey = "21AD"

      val vatReturnOverdue = vatReturn.copy(periodKey = periodOverdueKey)
      val vatReturnDue = vatReturn.copy(periodKey = periodDueKey)

      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodOverdueKey)))
        .thenReturn(Future.successful(Right(vatReturnOverdue)))
      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodDueKey)))
        .thenReturn(Future.successful(Right(vatReturnDue)))
      when(financialDataConnector.getFinancialData(any(), any()))
        .thenReturn(Future.successful[FinancialDataResponse](Right(Some(inputFinancialData))))

      val obligationsDetails = obligationsResponse.obligations.head.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodOverdueKey)
      val obligationsDetail2: EtmpObligationDetails = obligationsDetails(1).copy(periodKey = periodDueKey)

      val etmpObligation = obligationsResponse.obligations.head.copy(obligationDetails = List(obligationsDetail1, obligationsDetail2))

      when(vatReturnConnector.getObligations(any(), any()))
        .thenReturn(Future.successful(Right(obligationsResponse.copy(obligations = Seq(etmpObligation)))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector)

      val result = service.getUnpaidPayments(iossNumber, someCommencementDate)

      whenReady(result) { r =>
        val paymentOverdue = service.calculatePayment(vatReturnOverdue, Some(inputFinancialData))
        val paymentDue = service.calculatePayment(vatReturnDue, Some(inputFinancialData))

        r mustBe List(paymentOverdue, paymentDue)
        paymentOverdue.amountOwed mustBe (transactionAmountOverdue1 + transactionAmountOverdue2)
        paymentDue.amountOwed mustBe (transactionAmountDue1 + transactionAmountDue2)
      }
    }

    "must return correct payment when there are due payments - from FinancialData, vatReturn to be ignored" in {

      val periodDue = Period(2021, Month.SEPTEMBER)

      val transactionAmount1 = 250
      val transactionAmount2 = 750

      val ft1 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount1)))
      val ft2 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount2)))

      val inputFinancialData = financialData.copy(financialTransactions = Some(List(ft1, ft2)))
      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val periodKey1 = "21AI"

      val vatReturn1 = vatReturn.copy(periodKey = periodKey1)

      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodKey1)))
        .thenReturn(Future.successful(Right(vatReturn1)))

      when(financialDataConnector.getFinancialData(any(), any())).thenReturn(Future.successful(Right(Some(inputFinancialData))))

      val obligationsDetails = obligationsResponse.obligations.head.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodKey1)

      val etmpObligation = obligationsResponse.obligations.head.copy(obligationDetails = List(obligationsDetail1))

      when(vatReturnConnector.getObligations(any(), any()))
        .thenReturn(Future.successful(Right(obligationsResponse.copy(obligations = Seq(etmpObligation)))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector)

      val payment = service.calculatePayment(vatReturn1, Some(inputFinancialData))

      val result: Future[List[Payment]] = service.getUnpaidPayments(iossNumber, someCommencementDate)

      whenReady(result) { r =>
        r mustBe List(payment)
        payment.amountOwed mustBe (transactionAmount1 + transactionAmount2)
      }
    }

    "must return payments when there are overdue payments - from FinancialData, vatReturn to be ignored" in {

      val periodOverdue1 = Period(2021, Month.JUNE)
      val periodOverdue2 = Period(2021, Month.SEPTEMBER)

      val periodKey1 = "21AI"
      val periodKey2 = "21AF"

      val transactionAmount1 = 250
      val transactionAmount2 = 750
      val transactionAmount3 = 300
      val transactionAmount4 = 700

      val ft1 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue1.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount1)))
      val ft2 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue1.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount2)))
      val ft3 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue2.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount3)))
      val ft4 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue2.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount4)))

      val inputFinancialData = financialData.copy(financialTransactions = Some(List(ft1, ft2, ft3, ft4)))

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val vatReturn1 = vatReturn.copy(periodKey = periodKey1)
      val vatReturn2 = vatReturn.copy(periodKey = periodKey2)

      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodKey1)))
        .thenReturn(Future.successful(Right(vatReturn1)))
      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodKey2)))
        .thenReturn(Future.successful(Right(vatReturn2)))
      when(financialDataConnector.getFinancialData(any(), any())).thenReturn(Future.successful[FinancialDataResponse](Right(Some(inputFinancialData))))

      val obligationsDetails = obligationsResponse.obligations.head.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodKey1)
      val obligationsDetail2: EtmpObligationDetails = obligationsDetails(1).copy(periodKey = periodKey2)

      val etmpObligation = obligationsResponse.obligations.head.copy(obligationDetails = List(obligationsDetail1, obligationsDetail2))

      when(vatReturnConnector.getObligations(any(), any()))
        .thenReturn(Future.successful(Right(obligationsResponse.copy(obligations = Seq(etmpObligation)))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector)

      val result = service.getUnpaidPayments(iossNumber, someCommencementDate)

      val payment1 = service.calculatePayment(vatReturn1, Some(inputFinancialData))
      val payment2 = service.calculatePayment(vatReturn2, Some(inputFinancialData))

      whenReady(result) { r =>
        r mustBe (List(payment1, payment2))
        payment1.amountOwed mustBe (transactionAmount1 + transactionAmount2)
        payment2.amountOwed mustBe (transactionAmount3 + transactionAmount4)
      }
    }

    "must return payments when there are overdue payments - from FinancialData with no transactions, vatReturn to be taken into account" - {

      val periodKey1 = "21AI"
      val periodKey2 = "21AF"

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val vatReturn1 = vatReturn.copy(periodKey = periodKey1)
      val vatReturn2 = vatReturn.copy(periodKey = periodKey2)

      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodKey1)))
        .thenReturn(Future.successful(Right(vatReturn1)))
      when(vatReturnConnector.get(iossNumber, Period.fromKey(periodKey2)))
        .thenReturn(Future.successful(Right(vatReturn2)))

      val obligationsDetails = obligationsResponse.obligations.head.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodKey1)
      val obligationsDetail2: EtmpObligationDetails = obligationsDetails(1).copy(periodKey = periodKey2)

      val etmpObligation = obligationsResponse.obligations.head.copy(obligationDetails = List(obligationsDetail1, obligationsDetail2))

      when(vatReturnConnector.getObligations(any(), any()))
        .thenReturn(Future.successful(Right(obligationsResponse.copy(obligations = Seq(etmpObligation)))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector)

      val inputFinancialDataWithTransactionsNone = financialData.copy(financialTransactions = None)
      val inputFinancialDataWithTransactionsSomeNil = financialData.copy(financialTransactions = Some(Nil))
      val scenarios = Table[FinancialData, String](
        ("FinancialData", "Title"),
        (inputFinancialDataWithTransactionsNone, "inputFinancialDataWithTransactionsNone"),
        (inputFinancialDataWithTransactionsSomeNil, "inputFinancialDataWithTransactionsSomeNil")
      )

      forAll(scenarios) { (inputFinancialData, title) => {
        when(financialDataConnector.getFinancialData(any(), any())).thenReturn(Future.successful(Right(Some(inputFinancialData))))

        val result = service.getUnpaidPayments(iossNumber, someCommencementDate)

        val payment1 = service.calculatePayment(vatReturn1, Some(inputFinancialData))
        val payment2 = service.calculatePayment(vatReturn2, Some(inputFinancialData))

        s"when $title" in {
          whenReady(result) { r =>
            r mustBe (List(payment1, payment2))
            payment1.amountOwed mustBe vatReturn1.totalVATAmountDueForAllMSGBP
            payment2.amountOwed mustBe vatReturn2.totalVATAmountDueForAllMSGBP
          }
        }
      }
      }
    }

    "filterIfPaymentOutstanding correctly: " +
      "if some payments 'FOR THAT PERIOD' made with outstanding amount " +
      "or no payments made 'FOR THAT PERIOD' but there is vat amount 'FOR THAT PERIOD'" in {

      val inputFinancialDataWithNoTransactions = financialData.copy(financialTransactions = None)

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val periodKey1 = "21AI"

      val nilVatReturn = vatReturn.copy(
        periodKey = periodKey1, goodsSupplied = Nil, totalVATAmountDueForAllMSGBP = BigDecimal(0))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector)

      val vatCorrectionAmount = 50

      val vatReturnWithNoGoodsSuppliedAndSomeCorrection = nilVatReturn
        .copy(totalVATAmountDueForAllMSGBP = BigDecimal(vatCorrectionAmount))

      val transactionAmount1 = 250

      val transactionNotInPeriod = financialTransaction
        .copy(taxPeriodFrom = Some(Period.fromKey(periodKey1).firstDay.minusMonths(1)), outstandingAmount = Some(BigDecimal(transactionAmount1)))
      val transactionInPeriod = financialTransaction
        .copy(taxPeriodFrom = Some(Period.fromKey(periodKey1).firstDay), outstandingAmount = Some(BigDecimal(transactionAmount1)))

      val inputFinancialDataWithSomeTransactionsButNotInPeriod = inputFinancialDataWithNoTransactions
        .copy(financialTransactions = Some(List(transactionNotInPeriod)))
      val inputFinancialDataWithSomeTransactionsInPeriod = inputFinancialDataWithNoTransactions.copy(financialTransactions = Some(List(transactionInPeriod)))

      val scenarios = Table[Option[FinancialData], List[EtmpVatReturn], List[EtmpVatReturn]](
        ("FinancialData", "EtmpVatReturn", "Filtered EtmpVatReturn"),
        (
          Some(inputFinancialDataWithNoTransactions),
          List(nilVatReturn),
          Nil
        ),
        (
          Some(inputFinancialDataWithNoTransactions),
          List(vatReturnWithNoGoodsSuppliedAndSomeCorrection),
          List(vatReturnWithNoGoodsSuppliedAndSomeCorrection)
        ),
        (
          Some(inputFinancialDataWithSomeTransactionsButNotInPeriod),
          List(nilVatReturn),
          Nil
        ),
        (
          Some(inputFinancialDataWithSomeTransactionsInPeriod),
          List(nilVatReturn),
          List(nilVatReturn)
        )
      )

      forAll(scenarios) { (fd, vr, r) =>
        val result = service.filterIfPaymentOutstanding(fd, vr)
        result mustBe r
      }
    }
  }
}

trait PaymentsServiceSpecFixture {
  protected val zonedNow: ZonedDateTime = ZonedDateTime.of(
    2023,
    2,
    1,
    0,
    0,
    0,
    0,
    ZoneOffset.UTC
  )

  protected val zonedDateTimeNow = ZonedDateTime.now().plusSeconds(1)

  protected val dateFrom: LocalDate = zonedNow.toLocalDate.minusMonths(1)
  protected val dateTo: LocalDate = zonedNow.toLocalDate

  protected val item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))
  protected val financialTransaction = FinancialTransaction(
    Some("G Ret AT EU-OMS"), None, Some(dateFrom), Some(dateTo), Some(1000), Some(500), Some(500), Some(Seq(item)))

  protected val vatReturn: EtmpVatReturn = EtmpVatReturn(
    returnReference = "XI/IM9001234567/2023.M11",
    returnVersion = LocalDateTime.of(2023, 1, 1, 0, 0),
    periodKey = "23AK",
    returnPeriodFrom = LocalDate.of(2023, 1, 1),
    returnPeriodTo = LocalDate.of(2023, 1, 31),
    goodsSupplied = Seq(
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "FR",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(12345.67),
        vatAmountGBP = BigDecimal(2469.13)
      )
    ),
    totalVATGoodsSuppliedGBP = BigDecimal(2469.13),
    totalVATAmountPayable = BigDecimal(2469.13),
    totalVATAmountPayableAllSpplied = BigDecimal(2469.13),
    correctionPreviousVATReturn = Seq(
      EtmpVatReturnCorrection(
        periodKey = "23AJ",
        periodFrom = LocalDate.of(2023, 1, 1).toString,
        periodTo = LocalDate.of(2023, 1, 31).toString,
        msOfConsumption = "FR",
        0,
        0
      )
    ),
    totalVATAmountFromCorrectionGBP = BigDecimal(100.00),
    balanceOfVATDueForMS = Seq(
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "FR",
        totalVATDueGBP = BigDecimal(2569.13),
        totalVATEUR = BigDecimal(2569.13)
      )
    ),
    totalVATAmountDueForAllMSGBP = BigDecimal(2569.13),
    paymentReference = "XI/IM9001234567/2023.M11"
  )

  protected val obligationsResponse = EtmpObligations(obligations = Seq(EtmpObligation(
    obligationDetails = Seq(
      EtmpObligationDetails(
        status = EtmpObligationsFulfilmentStatus.Fulfilled,
        periodKey = "23AL"
      ),
      EtmpObligationDetails(
        status = EtmpObligationsFulfilmentStatus.Fulfilled,
        periodKey = "23AK"
      ),
      EtmpObligationDetails(
        status = EtmpObligationsFulfilmentStatus.Open, //To Be Ignored, this tests the filtering
        periodKey = "23AJ"
      )
    )
  )))

  protected val financialData = FinancialData(Some("IOSS"), Some("123456789"), Some("ECOM"), zonedDateTimeNow, Some(Seq(financialTransaction)))
}