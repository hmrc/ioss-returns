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

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.iossreturns.models.{CoreCorrection, CoreMsconSupply, CorePeriod, CoreSupply, CoreTraderId, CoreVatReturn, Period}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}

import java.math.MathContext
import java.time.{Instant, LocalDate, LocalDateTime, Month, ZonedDateTime, ZoneId}
import java.time.temporal.ChronoUnit
import scala.math.BigDecimal.RoundingMode

trait ModelGenerators {
  self: Generators =>

  implicit val arbitraryPeriod: Arbitrary[Period] =
    Arbitrary {
      for {
        year <- Gen.choose(2022, 2099)
        quarter <- Gen.oneOf(Month.values)
      } yield Period(year, quarter)
    }

  implicit val arbitraryCoreTraderId: Arbitrary[CoreTraderId] =
    Arbitrary {
      for {
        iossNumber <- arbitrary[String]
        issuedBy <- arbitrary[String]
      } yield CoreTraderId(iossNumber, issuedBy)
    }

  implicit val arbitraryCorePeriod: Arbitrary[CorePeriod] =
    Arbitrary {
      for {
        year <- arbitrary[Int]
        month <- arbitrary[Int]
      } yield CorePeriod(year, month)
    }

  implicit val arbitraryBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary {
      for {
        nonDecimalNumber <- arbitrary[Int]
        decimalNumber <- arbitrary[Int].retryUntil(_ > 0).retryUntil(_.toString.reverse.head.toString != "0")
      } yield BigDecimal(s"$nonDecimalNumber.$decimalNumber")
    }

  implicit val arbitraryCoreSupply: Arbitrary[CoreSupply] = {
    Arbitrary {
      for {
        supplyType <- arbitrary[String]
        vatRate <- arbitrary[BigDecimal]
        vatRateType <- arbitrary[String]
        taxableAmountGBP <- arbitrary[BigDecimal]
        vatAmountGBP <- arbitrary[BigDecimal]
      } yield CoreSupply(
        supplyType = supplyType,
        vatRate = vatRate,
        vatRateType = vatRateType,
        taxableAmountGBP = taxableAmountGBP,
        vatAmountGBP = vatAmountGBP
      )
    }
  }

  implicit val arbitraryCoreCorrection: Arbitrary[CoreCorrection] = {
    Arbitrary {
      for {
        corePeriod <- arbitrary[CorePeriod]
        totalVatAmountCorrectionGBP <- arbitrary[BigDecimal]
      } yield CoreCorrection(
        period = corePeriod,
        totalVatAmountCorrectionGBP = totalVatAmountCorrectionGBP
      )
    }
  }

  implicit val arbitraryCoreMsconSupply: Arbitrary[CoreMsconSupply] =
    Arbitrary {
      for {
        msconCountryCode <- arbitrary[String]
        balanceOfVatDueGBP <- arbitrary[BigDecimal]
        grandTotalMsidGoodsGBP <- arbitrary[BigDecimal]
        grandTotalMsestGoodsGBP <- arbitrary[BigDecimal]
        correctionsTotalGBP <- arbitrary[BigDecimal]
        amountOfMsidSupplies <- Gen.oneOf(List(1, 2, 3))
        msidSupplies <- Gen.listOfN(amountOfMsidSupplies, arbitrary[CoreSupply])
        amountOfCorrections <- Gen.oneOf(List(1, 2, 3))
        corrections <- Gen.listOfN(amountOfCorrections, arbitrary[CoreCorrection])
      } yield CoreMsconSupply(
        msconCountryCode = msconCountryCode,
        balanceOfVatDueGBP = balanceOfVatDueGBP,
        grandTotalMsidGoodsGBP = grandTotalMsidGoodsGBP,
        grandTotalMsestGoodsGBP = grandTotalMsestGoodsGBP,
        correctionsTotalGBP = correctionsTotalGBP,
        msidSupplies = msidSupplies,
        msestSupplies = List.empty,
        corrections = corrections
      )
    }

  implicit val arbitraryCoreVatReturn: Arbitrary[CoreVatReturn] =
    Arbitrary {
      for {
        vatReturnReferenceNumber <- arbitrary[String]
        version <- arbitrary[Instant]
        coreTraderId <- arbitrary[CoreTraderId]
        corePeriod <- arbitrary[CorePeriod]
        startDate <- arbitrary[LocalDate]
        endDate <- arbitrary[LocalDate]
        submissionDateTime <- arbitrary[Instant]
        totalAmountVatDueGBP <- arbitrary[BigDecimal]
        amountOfMsconSupplies <- Gen.oneOf(List(1, 2, 3))
        msconSupplies <- Gen.listOfN(amountOfMsconSupplies, arbitrary[CoreMsconSupply])
        changeDate <- arbitrary[LocalDateTime].map(x => x.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Z")).withYear(2023).truncatedTo(ChronoUnit.MILLIS).toLocalDateTime)

      } yield CoreVatReturn(
        vatReturnReferenceNumber = vatReturnReferenceNumber,
        version = version,
        traderId = coreTraderId,
        period = corePeriod,
        startDate = startDate,
        endDate = endDate,
        submissionDateTime = submissionDateTime,
        totalAmountVatDueGBP = totalAmountVatDueGBP,
        msconSupplies = msconSupplies,
        changeDate = Some(changeDate)
      )
    }

  implicit val arbitraryFinancialData: Arbitrary[FinancialData] = Arbitrary {
    for {
      idType <- arbitrary[String]
      idNumber <- arbitrary[Int]
      regimeType <- arbitrary[String]
      processingDate <- arbitrary[ZonedDateTime]
      financialTransaction <- arbitrary[FinancialTransaction]
    } yield FinancialData(Some(idType), Some(idNumber.toString), Some(regimeType), processingDate.withNano(0), Some(List(financialTransaction)))
  }

  implicit val arbitraryItem: Arbitrary[Item] = Arbitrary {
    for {
      amount <- arbitrary[BigDecimal]
      clearingReason <- arbitrary[String]
      paymentReference <- arbitrary[String]
      paymentAmount <- arbitrary[BigDecimal]
      paymentMethod <- arbitrary[String]
    } yield Item(
      Some(amount.setScale(2, RoundingMode.HALF_UP)),
      Some(clearingReason),
      Some(paymentReference),
      Some(paymentAmount.setScale(2, RoundingMode.HALF_UP)),
      Some(paymentMethod)
    )
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
    } yield FinancialTransaction(
      Some(chargeType),
      Some(mainType),
      Some(taxPeriodFrom),
      Some(taxPeriodTo),
      Some(originalAmount.setScale(2, RoundingMode.HALF_UP)),
      Some(outstandingAmount.setScale(2, RoundingMode.HALF_UP)),
      Some(clearedAmount.setScale(2, RoundingMode.HALF_UP)),
      Some(List(item))
    )
  }

}
