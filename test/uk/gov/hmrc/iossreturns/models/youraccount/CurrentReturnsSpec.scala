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

package uk.gov.hmrc.iossreturns.models.youraccount

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.StandardPeriod

import java.time.Month


class CurrentReturnsSpec
  extends SpecBase with Matchers{

  "CurrentReturns" - {

    "serialize and deserialize successfully" in {
      val period = StandardPeriod(2023, Month.APRIL)
      val return1 = Return.fromPeriod(period, SubmissionStatus.Complete, inProgress = false, isOldest = true)
      val return2 = Return.fromPeriod(period, SubmissionStatus.Due, inProgress = true, isOldest = false)

      val model = CurrentReturns(
        returns = Seq(return1, return2),
        excluded = false,
        finalReturnsCompleted = true,
        iossNumber = iossNumber,
        completeOrExcludedReturns = Seq(return1)
      )

      val json = Json.toJson(model)

      val deserialized = json.as[CurrentReturns]

      deserialized mustBe model
    }

    "fail deserialization when 'returns' is missing" in {
      val json = Json.parse(
        """
          |{
          |  "excluded": false,
          |  "finalReturnsCompleted": true,
          |  "iossNumber": "IM9001234567",
          |  "completeOrExcludedReturns": []
          |}
            """.stripMargin
      )

      intercept[JsResultException] {
        json.as[CurrentReturns]
      }
    }

    "fail deserialization when 'iossNumber' is missing" in {
      val json = Json.parse(
        """
          |{
          |  "returns": [],
          |  "excluded": false,
          |  "finalReturnsCompleted": true,
          |  "completeOrExcludedReturns": []
          |}
            """.stripMargin
      )

      intercept[JsResultException] {
        json.as[CurrentReturns]
      }
    }

    "fail deserialization for empty JSON" in {
      val emptyJson = Json.parse("{}")

      intercept[JsResultException] {
        emptyJson.as[CurrentReturns]
      }
    }

    "serialize an empty CurrentReturns model correctly" in {
      val model = CurrentReturns(
        returns = Seq.empty,
        excluded = false,
        finalReturnsCompleted = false,
        iossNumber = "",
        completeOrExcludedReturns = Seq.empty
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "returns": [],
          |  "excluded": false,
          |  "finalReturnsCompleted": false,
          |  "iossNumber": "",
          |  "completeOrExcludedReturns": []
          |}
            """.stripMargin
      )

      val json = Json.toJson(model)

      json mustBe expectedJson
    }
  }
}

