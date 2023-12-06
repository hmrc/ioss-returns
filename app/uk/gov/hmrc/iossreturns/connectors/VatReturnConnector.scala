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
import uk.gov.hmrc.iossreturns.config.{CoreVatReturnConfig, EtmpDisplayReturnConfig}
import uk.gov.hmrc.iossreturns.connectors.CoreVatReturnHttpParser.{CoreVatReturnReads, CoreVatReturnResponse}
import uk.gov.hmrc.iossreturns.connectors.EtmpDisplayVatReturnHttpParser.{EtmpVatReturnReads, EtmpDisplayVatReturnResponse}
import uk.gov.hmrc.iossreturns.models.{CoreErrorResponse, CoreVatReturn, EisErrorResponse, Period}
import uk.gov.hmrc.iossreturns.models.GatewayTimeout

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnConnector @Inject()(
                                    httpClient: HttpClient,
                                    coreVatReturnConfig: CoreVatReturnConfig,
                                    etmpDisplayReturnConfig: EtmpDisplayReturnConfig
                                  )(implicit ec: ExecutionContext) extends Logging {

  private implicit val emptyHc: HeaderCarrier = HeaderCarrier()

  private def submissionHeaders(correlationId: String): Seq[(String, String)] = coreVatReturnConfig.submissionHeaders(correlationId)

  private def displayHeaders(correlationId: String): Seq[(String, String)] = etmpDisplayReturnConfig.headers(correlationId)


  def submit(coreVatReturn: CoreVatReturn): Future[CoreVatReturnResponse] = {
    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = submissionHeaders(correlationId)

    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    def url = s"${coreVatReturnConfig.baseUrl}"

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

  def get(iossNumber: String, period: Period): Future[EtmpDisplayVatReturnResponse] = {
    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = displayHeaders(correlationId)

    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    def url = s"${etmpDisplayReturnConfig.baseUrl}/$iossNumber/${period.toEtmpPeriodString}"

    logger.info(s"Sending get request to ETMP with headers $headersWithoutAuth")

    httpClient.GET[EtmpDisplayVatReturnResponse](
      url,
      headers = headersWithCorrelationId
    ).recover {
      case e: HttpException =>
        logger.error(s"Unexpected error response from core $url, received status ${e.responseCode}, body of response was: ${e.message}")
        Left(GatewayTimeout)
    }
  }

}
