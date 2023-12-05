/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.iossreturns.generators

import models._
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}

import java.time.{LocalDate, ZonedDateTime}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.iossreturns.models.IOSSNumber

trait ModelGenerators {
  implicit val arbitraryFinancialData: Arbitrary[FinancialData] = Arbitrary {
    for {
      idType <- arbitrary[String]
      idNumber <- arbitrary[Int]
      regimeType <- arbitrary[String]
      processingData <- arbitrary[ZonedDateTime]
      financialTransaction <- arbitrary[FinancialTransaction]
    } yield FinancialData(Some(idType), Some(idNumber.toString), Some(regimeType), processingData, Some(List(financialTransaction)))
  }

  implicit val arbitraryItem: Arbitrary[Item] = Arbitrary {
    for {
      amount <- arbitrary[BigDecimal]
      clearingReason <- arbitrary[String]
      paymentReference <- arbitrary[String]
      paymentAmount <- arbitrary[BigDecimal]
      paymentMethod <- arbitrary[String]
    } yield Item(Some(amount), Some(clearingReason), Some(paymentReference), Some(paymentAmount), Some(paymentMethod))
  }

  implicit val arbitraryFinancialTransaction: Arbitrary[FinancialTransaction] = Arbitrary {
    for {
      chargeType <- arbitrary[String]
      mainType <- arbitrary[String]
      taxPeriodFrom <- arbitrary[LocalDate]
      taxPeriodTo <- arbitrary[LocalDate]
      originalAmount <- arbitrary[BigDecimal]
      outstandingAmount <- arbitrary[BigDecimal]
      clearedAmount <- arbitrary[BigDecimal]
      item <- arbitrary[Item]
    } yield FinancialTransaction(Some(chargeType), Some(mainType), Some(taxPeriodFrom), Some(taxPeriodTo), Some(originalAmount), Some(outstandingAmount), Some(clearedAmount), Some(List(item)))
  }

  implicit val arbitraryIOSSNumber: Arbitrary[IOSSNumber] =
    Arbitrary {
      Gen.listOfN(9, Gen.numChar).map(_.mkString).map(IOSSNumber(_))
    }
}
