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

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import uk.gov.hmrc.iossreturns.connectors.RegistrationConnector
import uk.gov.hmrc.iossreturns.models.EtmpRegistration
import uk.gov.hmrc.iossreturns.models.requests.{IdentifierRequest, RegistrationRequest}
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FakeGetRegistrationAction(etmpRegistration: EtmpRegistration)
  extends GetRegistrationAction(mock[RegistrationConnector]) {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
    Right(RegistrationRequest(request.request, request.credentials, request.vrn, request.iossNumber, etmpRegistration)).toFuture
}
