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
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.option
import uk.gov.hmrc.iossreturns.models._
import uk.gov.hmrc.iossreturns.models.etmp._
import uk.gov.hmrc.iossreturns.models.financialdata.{FinancialData, FinancialTransaction, Item}

import java.time.{Instant, LocalDate, LocalDateTime, Month, ZoneId, ZonedDateTime}
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

  implicit val arbitraryEtmpVatReturnGoodsSupply: Arbitrary[EtmpVatReturnGoodsSupplied] =
    Arbitrary {
      for {
        msOfConsumption <- arbitrary[String]
        vatRateType <- Gen.oneOf(EtmpVatRateType.values)
        taxableAmountGBP <- arbitrary[BigDecimal]
        vatAmountGBP <- arbitrary[BigDecimal]
      } yield EtmpVatReturnGoodsSupplied(
        msOfConsumption = msOfConsumption,
        vatRateType = vatRateType,
        taxableAmountGBP = taxableAmountGBP,
        vatAmountGBP = vatAmountGBP
      )
    }

  implicit val arbitraryEtmpVatReturnCorrection: Arbitrary[EtmpVatReturnCorrection] =
    Arbitrary {
      for {
        periodKey <- arbitrary[String]
        periodFrom <- arbitrary[String]
        periodTo <- arbitrary[String]
        msOfConsumption <- arbitrary[String]
      } yield EtmpVatReturnCorrection(
        periodKey = periodKey,
        periodFrom = periodFrom,
        periodTo = periodTo,
        msOfConsumption = msOfConsumption
      )
    }

  implicit val arbitraryEtmpVatReturnBalanceOfVatDue: Arbitrary[EtmpVatReturnBalanceOfVatDue] =
    Arbitrary {
      for {
        msOfConsumption <- arbitrary[String]
        totalVATDueGBP <- arbitrary[BigDecimal]
        totalVATEUR <- arbitrary[BigDecimal]
      } yield EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = msOfConsumption,
        totalVATDueGBP = totalVATDueGBP,
        totalVATEUR = totalVATEUR
      )
    }

  implicit val arbitraryEtmpVatReturn: Arbitrary[EtmpVatReturn] =
    Arbitrary {
      for {
        returnReference <- arbitrary[String]
        periodKey <- arbitrary[String]
        returnPeriodFrom <- arbitrary[LocalDate]
        returnPeriodTo <- arbitrary[LocalDate]
        amountOfGoodsSupplied <- Gen.oneOf(List(1, 2, 3))
        goodsSupplied <- Gen.listOfN(amountOfGoodsSupplied, arbitrary[EtmpVatReturnGoodsSupplied])
        totalVATGoodsSuppliedGBP <- arbitrary[BigDecimal]
        totalVATAmountPayable <- arbitrary[BigDecimal]
        totalVATAmountPayableAllSpplied <- arbitrary[BigDecimal]
        amountOfCorrections <- Gen.oneOf(List(1, 2, 3))
        correctionPreviousVATReturn <- Gen.listOfN(amountOfCorrections, arbitrary[EtmpVatReturnCorrection])
        totalVATAmountFromCorrectionGBP <- arbitrary[BigDecimal]
        amountOfBalanceOfVATDueForMS <- Gen.oneOf(List(1, 2, 3))
        balanceOfVATDueForMS <- Gen.listOfN(amountOfBalanceOfVATDueForMS, arbitrary[EtmpVatReturnBalanceOfVatDue])
        totalVATAmountDueForAllMSEUR <- arbitrary[BigDecimal]
        paymentReference <- arbitrary[String]
      } yield EtmpVatReturn(
        returnReference = returnReference,
        periodKey = periodKey,
        returnPeriodFrom = returnPeriodFrom,
        returnPeriodTo = returnPeriodTo,
        goodsSupplied = goodsSupplied,
        totalVATGoodsSuppliedGBP = totalVATGoodsSuppliedGBP,
        totalVATAmountPayable = totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = correctionPreviousVATReturn,
        totalVATAmountFromCorrectionGBP = totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = balanceOfVATDueForMS,
        totalVATAmountDueForAllMSEUR = totalVATAmountDueForAllMSEUR,
        paymentReference = paymentReference
      )
    }

  implicit val arbitraryObligationDetails: Arbitrary[EtmpObligationDetails] =
    Arbitrary {
      for {
        status <- Gen.oneOf(EtmpObligationsFulfilmentStatus.values)
        periodKey <- arbitrary[String]
      } yield EtmpObligationDetails(
        status = status,
        periodKey = periodKey
      )
    }

  implicit val arbitraryObligations: Arbitrary[EtmpObligations] =
    Arbitrary {
      for {
        referenceNumber <- arbitrary[String]
        obligationDetails <- Gen.listOfN(3, arbitrary[EtmpObligationDetails])
      } yield {
        EtmpObligations(
          referenceNumber = referenceNumber,
          referenceType = "IOSS",
          obligationDetails = obligationDetails
        )
      }
    }

  implicit lazy val arbitraryEtmpTradingName: Arbitrary[EtmpTradingName] =
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield EtmpTradingName(tradingName)
    }

  implicit lazy val arbitraryEtmpBankDetails: Arbitrary[EtmpBankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield EtmpBankDetails(accountName, bic, iban)
    }

  implicit lazy val arbitraryEtmpWebsite: Arbitrary[EtmpWebsite] =
    Arbitrary {
      for {
        websiteAddress <- Gen.alphaStr
      } yield EtmpWebsite(websiteAddress)
    }

  implicit lazy val arbitraryVatNumberTraderId: Arbitrary[VatNumberTraderId] =
    Arbitrary {
      for {
        vatNumber <- Gen.alphaNumStr
      } yield VatNumberTraderId(vatNumber)
    }

  implicit lazy val arbitraryTaxRefTraderID: Arbitrary[TaxRefTraderID] =
    Arbitrary {
      for {
        taxReferenceNumber <- Gen.alphaNumStr
      } yield TaxRefTraderID(taxReferenceNumber)
    }

  implicit lazy val arbitrarySchemeType: Arbitrary[SchemeType] =
    Arbitrary {
      Gen.oneOf(SchemeType.values)
    }

  implicit lazy val arbitraryDate: Arbitrary[LocalDate] =
    Arbitrary {
      datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2023, 12, 31))
    }

  implicit lazy val arbitraryEtmpExclusionReason: Arbitrary[EtmpExclusionReason] =
    Arbitrary {
      Gen.oneOf(EtmpExclusionReason.values)
    }

  implicit lazy val arbitraryEtmpExclusion: Arbitrary[EtmpExclusion] =
    Arbitrary {
      for {
        exclusionReason <- arbitraryEtmpExclusionReason.arbitrary
        effectiveDate <- arbitraryDate.arbitrary
        decisionDateDate <- arbitraryDate.arbitrary
        quarantine <- arbitrary[Boolean]
      } yield EtmpExclusion(
        exclusionReason = exclusionReason,
        effectiveDate = effectiveDate,
        decisionDate = decisionDateDate,
        quarantine = quarantine
      )
    }

  implicit lazy val arbitraryBic: Arbitrary[Bic] = {
    val asciiCodeForA = 65
    val asciiCodeForN = 78
    val asciiCodeForP = 80
    val asciiCodeForZ = 90

    Arbitrary {
      for {
        firstChars <- Gen.listOfN(6, Gen.alphaUpperChar).map(_.mkString)
        char7 <- Gen.oneOf(Gen.alphaUpperChar, Gen.choose(2, 9))
        char8 <- Gen.oneOf(
          Gen.choose(asciiCodeForA, asciiCodeForN).map(_.toChar),
          Gen.choose(asciiCodeForP, asciiCodeForZ).map(_.toChar),
          Gen.choose(0, 9)
        )
        lastChars <- Gen.option(Gen.listOfN(3, Gen.oneOf(Gen.alphaUpperChar, Gen.numChar)).map(_.mkString))
      } yield Bic(s"$firstChars$char7$char8${lastChars.getOrElse("")}").get
    }
  }

  implicit lazy val arbitraryIban: Arbitrary[Iban] = {
    Arbitrary {
      Gen.oneOf(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      ).map(v => Iban(v).right.get)
    }
  }

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.euCountries)
    }

  implicit lazy val arbitraryNonCompliantDetails: Arbitrary[NonCompliantDetails] =
    Arbitrary {
      for {
        nonCompliantReturns <- option(Gen.chooseNum(1, 2))
        nonCompliantPayments <- option(Gen.chooseNum(1, 2))
      } yield {
        NonCompliantDetails(
          nonCompliantReturns = nonCompliantReturns,
          nonCompliantPayments = nonCompliantPayments
        )
      }
    }

  implicit val arbitraryEtmpEuRegistrationDetails: Arbitrary[EtmpEuRegistrationDetails] = {
    Arbitrary {
      for {
        countryOfRegistration <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString.toUpperCase)
        traderId <- arbitrary[VatNumberTraderId]
        tradingName <- arbitrary[String]
        fixedEstablishmentAddressLine1 <- arbitrary[String]
        fixedEstablishmentAddressLine2 <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        regionOrState <- Gen.option(arbitrary[String])
        postcode <- Gen.option(arbitrary[String])
      } yield {
        EtmpEuRegistrationDetails(
          countryOfRegistration,
          traderId,
          tradingName,
          fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2,
          townOrCity,
          regionOrState,
          postcode
        )
      }
    }
  }

  implicit val arbitraryEtmpPreviousEURegistrationDetails: Arbitrary[EtmpPreviousEuRegistrationDetails] = {
    Arbitrary {
      for {
        issuedBy <- arbitrary[String]
        registrationNumber <- arbitrary[String]
        schemeType <- Gen.oneOf(SchemeType.values)
        intermediaryNumber <- Gen.option(arbitrary[String])
      } yield EtmpPreviousEuRegistrationDetails(issuedBy, registrationNumber, schemeType, intermediaryNumber)
    }
  }

  implicit val arbitraryEtmpSchemeDetails: Arbitrary[EtmpSchemeDetails] = {
    Arbitrary {
      for {
        commencementDate <- arbitrary[String]
        euRegistrationDetails <- Gen.listOfN(5, arbitraryEtmpEuRegistrationDetails.arbitrary)
        previousEURegistrationDetails <- Gen.listOfN(5, arbitraryEtmpPreviousEURegistrationDetails.arbitrary)
        websites <- Gen.listOfN(10, arbitrary[EtmpWebsite])
        contactName <- arbitrary[String]
        businessTelephoneNumber <- arbitrary[String]
        businessEmailId <- arbitrary[String]
        nonCompliantReturns <- Gen.option(arbitrary[Int].toString)
        nonCompliantPayments <- Gen.option(arbitrary[Int].toString)
      } yield
        EtmpSchemeDetails(
          commencementDate,
          euRegistrationDetails,
          previousEURegistrationDetails,
          websites,
          contactName,
          businessTelephoneNumber,
          businessEmailId,
          nonCompliantReturns,
          nonCompliantPayments
        )
    }
  }

  implicit lazy val arbitraryAdminUse: Arbitrary[EtmpAdminUse] =
    Arbitrary {
      for {
        changeDate <- arbitrary[LocalDateTime]
      } yield EtmpAdminUse(Some(changeDate))
    }

  implicit val arbitraryEtmpRegistration: Arbitrary[EtmpRegistration] = Arbitrary {
    for {
      etmpTradingNames <- Gen.listOfN(2, arbitraryEtmpTradingName.arbitrary)
      schemeDetails <- arbitrary[EtmpSchemeDetails]
      bankDetails <- arbitrary[EtmpBankDetails]
      exclusions <- Gen.listOfN(1, arbitraryEtmpExclusion.arbitrary)
      adminUse <- arbitrary[EtmpAdminUse]
    } yield EtmpRegistration(
      etmpTradingNames,
      schemeDetails,
      bankDetails,
      exclusions,
      adminUse
    )
  }
}
