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

package uk.gov.hmrc.iossreturns.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class NonCompliantDetailsSpec extends AnyFreeSpec with Matchers {

  "NonCompliantDetails" - {

    "deserialize correctly from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "nonCompliantReturns": 5,
          |  "nonCompliantPayments": 3
          |}
            """.stripMargin
      )

      val expectedDetails = NonCompliantDetails(
        nonCompliantReturns = Some(5),
        nonCompliantPayments = Some(3)
      )

      json.as[NonCompliantDetails] mustBe expectedDetails
    }

    "deserialize correctly when fields are missing (None)" in {
      val json = Json.parse(
        """
          |{
          |  "nonCompliantReturns": 5
          |}
            """.stripMargin
      )

      val expectedDetails = NonCompliantDetails(
        nonCompliantReturns = Some(5),
        nonCompliantPayments = None
      )

      json.as[NonCompliantDetails] mustBe expectedDetails
    }

    "deserialize correctly when all fields are missing (None)" in {
      val json = Json.parse(
        """
          |{}
            """.stripMargin
      )

      val expectedDetails = NonCompliantDetails(
        nonCompliantReturns = None,
        nonCompliantPayments = None
      )

      json.as[NonCompliantDetails] mustBe expectedDetails
    }
  }
}
