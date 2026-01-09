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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Unauthorized
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossreturns.models.enrolments.PreviousRegistration
import uk.gov.hmrc.iossreturns.services.PreviousRegistrationService
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckOwnIossNumberFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockPreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockIntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]

  class Harness(iossNumber: String, previousRegistrationService: PreviousRegistrationService, intermediaryRegistrationConnector: IntermediaryRegistrationConnector) extends
    CheckOwnIossNumberFilterImpl(iossNumber, previousRegistrationService, intermediaryRegistrationConnector) {
    def callFilter(request: AuthorisedRequest[_]): Future[Option[Result]] = filter(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockPreviousRegistrationService)
  }

  ".filter" - {


    val registration = arbitraryEtmpRegistration.arbitrary.sample.value

    "must return None when the ioss number exists" in {

      val application = applicationBuilder(None).build()

      when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn
        List(PreviousRegistration(iossNumber = iossNumber, startPeriod = period, endPeriod = period)).toFuture

      running(application) {
        val request = AuthorisedRequest(FakeRequest(), userAnswersId, testCredentials.providerId, vrn, iossNumber, registration, None, enrolments)
        val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must redirect when there are no periods to be submitted" in {
      val application = applicationBuilder(None).build()

      when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn
        List.empty.toFuture

      running(application) {
        val request = AuthorisedRequest(FakeRequest(), userAnswersId, testCredentials.providerId, vrn, iossNumber, registration, None, enrolments)
        val controller = new Harness("IM9005444747", mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Unauthorized
      }
    }
  }

}
