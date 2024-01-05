/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.iossreturns

object Moo {

  def main(args: Array[String]): Unit = {

    import uk.gov.hmrc.iossreturns.models.RegistrationWrapper
    {import play.api.libs.json._
      val jsonText =
        """
          |{
          |  "vatInfo": {
          |    "desAddress": {
          |      "line1": "1 The Street",
          |      "line2": "Some Town",
          |      "postCode": "AA11 1AA",
          |      "countryCode": "GB"
          |    },
          |    "registrationDate": "2020-01-01",
          |    "partOfVatGroup": false,
          |    "organisationName": "Company Name",
          |    "singleMarketIndicator": true,
          |    "overseasIndicator": false
          |  },
          |  "registration": {
          |    "tradingNames": [
          |      {
          |        "tradingName": "tradingName1"
          |      },
          |      {
          |        "tradingName": "tradingName2"
          |      }
          |    ],
          |    "schemeDetails": {
          |      "commencementDate": "2024-01-02",
          |      "euRegistrationDetails": [
          |        {
          |          "countryOfRegistration": "DE",
          |          "traderId": {
          |            "vatNumber": "DE123456789"
          |          },
          |          "tradingName": "Some Trading Name",
          |          "fixedEstablishmentAddressLine1": "Line 1",
          |          "fixedEstablishmentAddressLine2": "Line 2",
          |          "townOrCity": "Town",
          |          "regionOrState": "Region",
          |          "postcode": "AB12 3CD"
          |        }
          |      ],
          |      "previousEURegistrationDetails": [
          |        {
          |          "issuedBy": "HU",
          |          "registrationNumber": "HU11122233",
          |          "schemeType": "OSS Union"
          |        },
          |        {
          |          "issuedBy": "HU",
          |          "registrationNumber": "EU111222333",
          |          "schemeType": "OSS Non-Union"
          |        },
          |        {
          |          "issuedBy": "HU",
          |          "registrationNumber": "IM3487777777",
          |          "schemeType": "IOSS with intermediary",
          |          "intermediaryNumber": "IM3487777777"
          |        },
          |        {
          |          "issuedBy": "CY",
          |          "registrationNumber": "IM1962223333",
          |          "schemeType": "IOSS without intermediary"
          |        }
          |      ],
          |      "websites": [
          |        {
          |          "websiteAddress": "www.website1.com"
          |        },
          |        {
          |          "websiteAddress": "www.website2.com"
          |        }
          |      ],
          |      "contactName": "Test name",
          |      "businessTelephoneNumber": "1234567890",
          |      "businessEmailId": "email@test.com",
          |      "nonCompliantReturns": "0",
          |      "nonCompliantPayments": "0"
          |    },
          |    "bankDetails": {
          |      "accountName": "Account name",
          |      "bic": "ABCDGB2A",
          |      "iban": "GB33BUKB20201555555555"
          |    },
          |    "exclusions": [],
          |    "adminUse": {
          |      "changeDate": "2024-01-02T12:05:24.860659"
          |    }
          |  }
          |}
          |""".stripMargin.trim


      val jsValue = Json.parse(jsonText)
      println(jsValue.validate[RegistrationWrapper])


    }
  }
}
