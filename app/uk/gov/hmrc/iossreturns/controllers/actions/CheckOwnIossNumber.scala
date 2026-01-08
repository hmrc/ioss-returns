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

package uk.gov.hmrc.iossreturns.controllers.actions

import play.api.mvc.{ActionFilter, Result}
import play.api.mvc.Results.Unauthorized
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossreturns.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.services.PreviousRegistrationService
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckOwnIossNumberFilterImpl(
                                    iossNumber: String,
                                    previousRegistrationService: PreviousRegistrationService,
                                    intermediaryRegistrationConnector: IntermediaryRegistrationConnector
                                  )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthorisedRequest] with Logging {

  override protected def filter[A](request: AuthorisedRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    if (request.iossNumber == iossNumber) {
      request.maybeIntermediaryNumber match {
        case Some(intermediaryNumber) =>
          intermediaryRegistrationConnector.get(intermediaryNumber).map { intermediaryRegistrationWrapper =>
            val availableIossNumbers = intermediaryRegistrationWrapper.etmpDisplayRegistration.clientDetails.map(_.clientIossID)
            if (availableIossNumbers.contains(iossNumber)) {
              None
            } else {
              logger.info(s"Intermediary $intermediaryNumber doesn't have access to ioss number $iossNumber")
              Some(Unauthorized)
            }
          }
        case _ =>
          None.toFuture
      }
    } else {
      previousRegistrationService.getPreviousRegistrations(request.credentialId).map { previousRegistrations =>
        val validIossNumbers: Seq[String] = request.iossNumber :: previousRegistrations.map(_.iossNumber)
        if (validIossNumbers.contains(iossNumber)) {
          None
        } else {
          Some(Unauthorized)
        }
      }
    }
  }
}

class CheckOwnIossNumberFilter @Inject()(
                                          previousRegistrationService: PreviousRegistrationService,
                                          intermediaryRegistrationConnector: IntermediaryRegistrationConnector
                                        )
                                        (implicit ec: ExecutionContext) {

  def apply(iossNumber: String): CheckOwnIossNumberFilterImpl =
    new CheckOwnIossNumberFilterImpl(iossNumber, previousRegistrationService, intermediaryRegistrationConnector)

}