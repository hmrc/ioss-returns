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

import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, StringContextOps}
import uk.gov.hmrc.iossreturns.config.{CoreVatReturnConfig, EtmpDisplayReturnConfig, EtmpListObligationsConfig}
import uk.gov.hmrc.iossreturns.connectors.CoreVatReturnHttpParser.{CoreVatReturnReads, CoreVatReturnResponse}
import uk.gov.hmrc.iossreturns.connectors.EtmpDisplayVatReturnHttpParser.{EtmpDisplayVatReturnResponse, EtmpVatReturnReads}
import uk.gov.hmrc.iossreturns.connectors.EtmpListObligationsHttpParser.{EtmpListObligationsReads, EtmpListObligationsResponse}
import uk.gov.hmrc.iossreturns.models.Period.toEtmpPeriodString
import uk.gov.hmrc.iossreturns.models._
import uk.gov.hmrc.iossreturns.models.etmp.EtmpObligationsQueryParameters

import java.net.URL
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnConnector @Inject()(
                                    httpClientV2: HttpClientV2,
                                    coreVatReturnConfig: CoreVatReturnConfig,
                                    etmpDisplayReturnConfig: EtmpDisplayReturnConfig,
                                    etmpListObligationsConfig: EtmpListObligationsConfig
                                  )(implicit ec: ExecutionContext) extends Logging {

  private implicit val emptyHc: HeaderCarrier = HeaderCarrier()

  private def submissionHeaders(correlationId: String): Seq[(String, String)] = coreVatReturnConfig.submissionHeaders(correlationId)

  private def displayHeaders(correlationId: String): Seq[(String, String)] = etmpDisplayReturnConfig.headers(correlationId)

  private def obligationsHeaders(correlationId: String): Seq[(String, String)] = etmpListObligationsConfig.headers(correlationId)


  def submit(coreVatReturn: CoreVatReturn): Future[CoreVatReturnResponse] = {
    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = submissionHeaders(correlationId)

    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    def url = url"${coreVatReturnConfig.baseUrl}"

    logger.info(s"Sending request to core with headers $headersWithoutAuth")

    httpClientV2.post(url).withBody(Json.toJson(coreVatReturn)).setHeader(headersWithCorrelationId: _*).execute[CoreVatReturnResponse].recover {
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

    def url = url"${etmpDisplayReturnConfig.baseUrl}/$iossNumber/${toEtmpPeriodString(period)}"

    logger.info(s"Sending get request to ETMP with headers $headersWithoutAuth")

    httpClientV2.get(url).setHeader(headersWithCorrelationId: _*).execute[EtmpDisplayVatReturnResponse].recover {
      case e: HttpException =>
        logger.error(s"Unexpected error response from core $url, received status ${e.responseCode}, body of response was: ${e.message}")
        Left(GatewayTimeout)
    }
  }

  private def getObligationsUrl(iossNumber: String): URL =
    url"${etmpListObligationsConfig.baseUrl}enterprise/obligation-data/${etmpListObligationsConfig.idType}/$iossNumber/${etmpListObligationsConfig.regimeType}"

  def getObligations(idNumber: String, queryParameters: EtmpObligationsQueryParameters): Future[EtmpListObligationsResponse] = {

    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = obligationsHeaders(correlationId)

    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    val url = getObligationsUrl(idNumber)

    logger.info(s"Sending getObligations request to ETMP with headers $headersWithoutAuth")

    httpClientV2.get(url)
      .setHeader(headersWithCorrelationId: _*)
      .transform(_.withQueryStringParameters(queryParameters.toSeqQueryParams: _*))
      .execute[EtmpListObligationsResponse].recover {
      case e: HttpException =>
        logger.error(s"Unexpected error response from ETMP $url, received status ${e.responseCode}, body of response was: ${e.message}")
        Left(GatewayTimeout)
    }
  }
}
