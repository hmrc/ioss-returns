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

package uk.gov.hmrc.iossreturns.models.payments

import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.StandardPeriod

import java.time.{LocalDate, Month}

class PrepareDataSpec extends SpecBase {

  "PrepareData" - {

    "deserialize correctly from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "duePayments": [
          |    {
          |      "period": { "year": 2023, "month": "M1" },
          |      "amountOwed": 100.00,
          |      "dateDue": "2023-02-28",
          |      "paymentStatus": "UNPAID"
          |    }
          |  ],
          |  "overduePayments": [
          |    {
          |      "period": { "year": 2022, "month": "M12" },
          |      "amountOwed": 50.00,
          |      "dateDue": "2022-12-31",
          |      "paymentStatus": "PARTIAL"
          |    }
          |  ],
          |  "excludedPayments": [],
          |  "totalAmountOwed": 150.00,
          |  "totalAmountOverdue": 50.00,
          |  "iossNumber": "XYZ123"
          |}
            """.stripMargin
      )

      val expectedPrepareData = PrepareData(
        duePayments = List(
          Payment(
            period = StandardPeriod(2023, Month.JANUARY),
            amountOwed = BigDecimal(100.00),
            dateDue = LocalDate.of(2023, 2, 28),
            paymentStatus = PaymentStatus.Unpaid
          )
        ),
        overduePayments = List(
          Payment(
            period = StandardPeriod(2022, Month.DECEMBER),
            amountOwed = BigDecimal(50.00),
            dateDue = LocalDate.of(2022, 12, 31),
            paymentStatus = PaymentStatus.Partial
          )
        ),
        excludedPayments = List(),
        totalAmountOwed = BigDecimal(150.00),
        totalAmountOverdue = BigDecimal(50.00),
        iossNumber = "XYZ123"
      )

      json.as[PrepareData] mustBe expectedPrepareData
    }
  }
}
