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

import play.api.Logging
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.services.AccountService
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends ActionBuilder[AuthorisedRequest, AnyContent] with ActionFunction[Request, AuthorisedRequest]

class AuthActionImpl @Inject()(
                                override val authConnector: AuthConnector,
                                val parser: BodyParsers.Default,
                                config: AppConfig,
                                registrationConnector: RegistrationConnector,
                                accountService: AccountService
                              )(implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(
      (AffinityGroup.Individual or AffinityGroup.Organisation) and
        CredentialStrength(CredentialStrength.strong)
    ).retrieve(
      Retrievals.credentials and
        Retrievals.internalId and
        Retrievals.allEnrolments and
        Retrievals.affinityGroup and
        Retrievals.confidenceLevel
    ) {

      case Some(credentials) ~ Some(internalId) ~ enrolments ~ Some(Organisation) ~ _ =>

        val futureMaybeIossNumber = findIossFromEnrolments(enrolments, credentials.providerId)

        futureMaybeIossNumber.flatMap { maybeIossNumber =>

          (findVrnFromEnrolments(enrolments), maybeIossNumber) match {
            case (Some(vrn), Some(latestIossNumber)) =>
              getRegistrationAndBlock(request, block, internalId, credentials.providerId, vrn, latestIossNumber)
            case _ =>
              logger.warn(s"Insufficient enrolments")
              throw InsufficientEnrolments("Insufficient enrolments")
          }
        }

      case Some(credentials) ~ Some(internalId) ~ enrolments ~ Some(Individual) ~ confidence =>
        val futureMaybeIossNumber = findIossFromEnrolments(enrolments, credentials.providerId)

        futureMaybeIossNumber.flatMap { maybeIossNumber =>

          (findVrnFromEnrolments(enrolments), maybeIossNumber) match {
            case (Some(vrn), Some(latestIossNumber)) =>
              if (confidence >= ConfidenceLevel.L250) {
                getRegistrationAndBlock(request, block, internalId, credentials.providerId, vrn, latestIossNumber)
              } else {
                logger.warn("Insufficient confidence level")
                throw InsufficientConfidenceLevel("Insufficient confidence level")
              }
            case _ =>
              logger.warn(s"Insufficient enrolments")
              throw InsufficientEnrolments("Insufficient enrolments")
          }
        }
      case _ =>
        logger.warn("Unable to retrieve authorisation data")
        throw new UnauthorizedException("Unable to retrieve authorisation data")
    } recover {
      case a: AuthorisationException =>
        logger.warn(s"Unauthorised given $a")
        Unauthorized
    }
  }

  private def getRegistrationAndBlock[A](
                                          request: Request[A],
                                          block: AuthorisedRequest[A] => Future[Result],
                                          internalId: String,
                                          credentialId: String,
                                          vrn: Vrn,
                                          latestIossNumber: String
                                        )(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      registrationWrapper <- registrationConnector.getRegistration()
      result <- block(AuthorisedRequest(request, internalId, credentialId, vrn, latestIossNumber, registrationWrapper.registration))
    } yield result
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

  private def findIossFromEnrolments(enrolments: Enrolments, credId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val filteredIossNumbers = enrolments.enrolments.filter(_.key == config.iossEnrolment).flatMap(_.identifiers.filter(_.key == "IOSSNumber").map(_.value)).toSeq

    filteredIossNumbers match {
      case firstEnrolment :: Nil => Some(firstEnrolment).toFuture
      case multipleEnrolments if multipleEnrolments.nonEmpty =>
        accountService.getLatestAccount(credId).map(x => Some(x))
      case _ => None.toFuture
    }
  }
}
