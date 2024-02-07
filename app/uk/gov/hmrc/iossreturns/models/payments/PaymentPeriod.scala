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

import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.time.Month

final case class PaymentPeriod(year: Int, month: Month)


object PaymentPeriod {

  val reads: Reads[PaymentPeriod] = {
    (
      (__ \ "year").read[Int] and
        (__ \ "month").read[String].map(m => Month.of(m.substring(1).toInt))
      )((year, month) => PaymentPeriod(year, month))
  }

  val writes: OWrites[PaymentPeriod] = {
    (
      (__ \ "year").write[Int] and
        (__ \ "month").write[String].contramap[Month](m => s"M${m.getValue}")
      )(unlift(PaymentPeriod.unapply))
  }

  implicit val format: OFormat[PaymentPeriod] = OFormat(reads, writes)
}