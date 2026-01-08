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

import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Unauthorized
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossreturns.models.requests.IntermediaryIdentifierRequest
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IntermediaryIdentifierAction
  extends ActionBuilder[IntermediaryIdentifierRequest, AnyContent]
    with ActionFunction[Request, IntermediaryIdentifierRequest]

class IntermediaryIdentifierActionImpl @Inject()(
                                                  override val authConnector: AuthConnector,
                                                  override val parser: BodyParsers.Default,
                                                  intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                                  config: AppConfig
                                                )(implicit val executionContext: ExecutionContext)
  extends IntermediaryIdentifierAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: IntermediaryIdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised().retrieve(Retrievals.internalId and Retrievals.allEnrolments) {
      case Some(internalId) ~ enrolments =>
        (findVrnFromEnrolments(enrolments), findIntermediaryNumberFromEnrolments(enrolments)) match {
          case (Some(vrn), Some(intermediaryNumber)) =>
            getRegistrationAndBlock(request, block, vrn, internalId, intermediaryNumber)

          case _ =>
            logger.warn(s"Insufficient enrolments")
            throw InsufficientEnrolments("Insufficient enrolments")
        }

      case _ =>
        logger.warn("Unable to retrieve authorisation data")
        throw new UnauthorizedException("Unable to retrieve authorisation data")
    }.recover {
      case a: AuthorisationException =>
        logger.warn(s"Unauthorised given $a")
        Unauthorized
    }
  }

  private def getRegistrationAndBlock[A](
                                          request: Request[A],
                                          block: IntermediaryIdentifierRequest[A] => Future[Result],
                                          vrn: Vrn,
                                          internalId: String,
                                          intermediaryNumber: String,
                                        )(implicit hc: HeaderCarrier): Future[Result] = {

    intermediaryRegistrationConnector.get(intermediaryNumber).flatMap { intermediaryRegistrationWrapper =>
      block(IntermediaryIdentifierRequest(request, internalId, vrn, intermediaryNumber, intermediaryRegistrationWrapper))
    }.recover { _ =>
      logger.error(s"Unable to retrieve Intermediary registration.")
      throw Exception(s"There was an error retrieving Intermediary registration.")
    }
  }

  private def findIntermediaryNumberFromEnrolments(enrolments: Enrolments): Option[String] = {
    val intermediaryEnrolmentKey: String = config.intermediaryEnrolmentIdentifierName
    enrolments.enrolments
      .find(_.key == config.intermediaryEnrolment)
      .flatMap(_.identifiers.find(id => id.key == intermediaryEnrolmentKey && id.value.nonEmpty).map(_.value))
  }

  private def findVrnFromEnrolments(enrolments: Enrolments): Option[Vrn] = {
    enrolments.enrolments.find(_.key == "HMRC-MTD-VAT")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VRN").map(e => Vrn(e.value))
      } orElse enrolments.enrolments.find(_.key == "HMCE-VATDEC-ORG")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VATRegNo").map(e => Vrn(e.value))
      }
  }
}
