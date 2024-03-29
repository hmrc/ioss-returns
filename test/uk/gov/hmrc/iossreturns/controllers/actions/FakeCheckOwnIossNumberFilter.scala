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
import uk.gov.hmrc.iossreturns.services.PreviousRegistrationService

import scala.concurrent.{ExecutionContext, Future}

class FakeCheckOwnIossNumberFilter(iossNumber: String) extends CheckOwnIossNumberFilterImpl(
  iossNumber,
  mock[PreviousRegistrationService]
)(ExecutionContext.Implicits.global) {
  override protected def filter[A](request: AuthorisedRequest[A]): Future[Option[Result]] = {
    Future.successful(None)
  }
}

class FakeCheckOwnIossNumberFilterProvider
  extends CheckOwnIossNumberFilter(mock[PreviousRegistrationService])(ExecutionContext.Implicits.global) {

  override def apply(iossNumber: String): CheckOwnIossNumberFilterImpl =
    new FakeCheckOwnIossNumberFilter(iossNumber)
}
