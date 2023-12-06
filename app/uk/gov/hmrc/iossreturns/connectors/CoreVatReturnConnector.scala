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

package uk.gov.hmrc.iossreturns.connectors

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException}
import uk.gov.hmrc.iossreturns.config.CoreVatReturnConfig
import uk.gov.hmrc.iossreturns.connectors.CoreVatReturnHttpParser.{CoreVatReturnReads, CoreVatReturnResponse}
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, CoreVatReturn, EisErrorResponse}

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CoreVatReturnConnector @Inject()(
                                        httpClient: HttpClient,
                                        coreVatReturnConfig: CoreVatReturnConfig
                                      )(implicit ec: ExecutionContext) extends Logging {

  private implicit val emptyHc: HeaderCarrier = HeaderCarrier()
  private def headers(correlationId: String): Seq[(String, String)] = coreVatReturnConfig.ifHeaders(correlationId)

  private def url = s"${coreVatReturnConfig.baseUrl}"

  def submit(coreVatReturn: CoreVatReturn): Future[CoreVatReturnResponse] = {
    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = headers(correlationId)

    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    logger.info(s"Sending request to core with headers $headersWithoutAuth")

    httpClient.POST[CoreVatReturn, CoreVatReturnResponse](
      url,
      coreVatReturn,
      headers = headersWithCorrelationId
    ).recover {
      case e: HttpException =>
        logger.error(s"Unexpected error response from core $url, received status ${e.responseCode}, body of response was: ${e.message}")
        Left(
          EisErrorResponse(
            CoreErrorResponse(Instant.now(), None, s"UNEXPECTED_${e.responseCode}", e.message)
          ))
    }
  }

}
