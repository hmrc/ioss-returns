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

package uk.gov.hmrc.iossreturns.config

import play.api.Configuration

import javax.inject.Inject

class FinancialDataConfig @Inject()(config: Configuration) {

  val baseUrl: Service = config.get[Service]("microservice.services.financial-data")
  val authorizationToken: String = config.get[String]("microservice.services.financial-data.authorizationToken")
  val environment: String = config.get[String]("microservice.services.financial-data.environment")
  val regimeType: String = config.get[String]("microservice.services.financial-data.regimeType")

<<<<<<< Updated upstream:app/uk/gov/hmrc/iossreturns/config/FinancialDataConfig.scala
  val financialDataHeaders: Seq[(String, String)] = Seq(
    "Authorization" -> s"Bearer $authorizationToken",
    "Environment" -> environment
=======
  private val XCorrelationId = "X-Correlation-Id"

  def submissionHeaders(correlationId: String): Seq[(String, String)] = Seq(
    CONTENT_TYPE -> MimeTypes.JSON,
    ACCEPT -> MimeTypes.JSON,
    AUTHORIZATION -> s"Bearer $authorizationToken",
    DATE -> Formatters.dateTimeFormatter.format(LocalDateTime.now(clock)),
    XCorrelationId -> correlationId,
    X_FORWARDED_HOST -> "MDTP"
>>>>>>> Stashed changes:app/uk/gov/hmrc/iossreturns/config/CoreVatReturnConfig.scala
  )
}