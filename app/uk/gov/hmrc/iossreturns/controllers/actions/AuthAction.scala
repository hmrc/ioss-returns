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

package uk.gov.hmrc.iossreturns.controllers.actions

import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import play.api.Logging
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends ActionBuilder[AuthorisedRequest, AnyContent] with ActionFunction[Request, AuthorisedRequest]

class AuthActionImpl @Inject()(
                                override val authConnector: AuthConnector,
                                val parser: BodyParsers.Default,
                                config: AppConfig
                              )(implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised().retrieve(Retrievals.internalId and Retrievals.allEnrolments) {

      case Some(internalId) ~ enrolments =>
        (findVrnFromEnrolments(enrolments), findIossFromEnrolments(enrolments)) match {
          case (Some(vrn), Some(iossNumber)) => block(AuthorisedRequest(request, internalId, vrn, iossNumber))
          case _ =>
            logger.warn(s"Insufficient enrolments")
            throw InsufficientEnrolments("Insufficient enrolments")
        }

      case _ =>
        logger.warn(s"Unable to retrieve authorisation data")
        throw new UnauthorizedException("Unable to retrieve authorisation data")
    } recover {
      case _: AuthorisationException =>
        logger.warn(s"Unauthorised given")
        Unauthorized
    }
  }

  private def findVrnFromEnrolments(enrolments: Enrolments): Option[Vrn] =
    enrolments.enrolments.find(_.key == "HMRC-MTD-VAT")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VRN").map(e => Vrn(e.value))
      } orElse enrolments.enrolments.find(_.key == "HMCE-VATDEC-ORG")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VATRegNo").map(e => Vrn(e.value))
      }

  private def findIossFromEnrolments(enrolments: Enrolments): Option[String] = {
    enrolments.enrolments.find(_.key == config.iossEnrolment).flatMap(_.identifiers.find(_.key == "IOSSNumber").map(_.value))
  }
}
