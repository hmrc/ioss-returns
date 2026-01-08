/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.iossreturns.models.enrolments

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.StandardPeriod

import java.time.Month

class PreviousRegistrationSpec extends SpecBase with Matchers {

  "PreviousRegistration" - {

    "serialize and deserialize correctly" in {
      val startPeriod = StandardPeriod(2021, Month.JANUARY)
      val endPeriod = StandardPeriod(2021, Month.DECEMBER)

      val previousRegistration = PreviousRegistration(
        iossNumber,
        startPeriod = startPeriod,
        endPeriod = endPeriod
      )

      val json = Json.toJson(previousRegistration)

      json mustBe Json.parse(
        """
          |{
          |  "iossNumber": "IM9001234567",
          |  "startPeriod": {
          |    "year": 2021,
          |    "month": "M1"
          |  },
          |  "endPeriod": {
          |    "year": 2021,
          |    "month": "M12"
          |  }
          |}
          |""".stripMargin
      )

      val parsed = json.as[PreviousRegistration]

      parsed mustBe previousRegistration
    }

  }
}
