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
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossreturns.models.enrolments.PreviousRegistration
import uk.gov.hmrc.iossreturns.models.etmp.intermediary.IntermediaryRegistrationWrapper
import uk.gov.hmrc.iossreturns.services.PreviousRegistrationService
import uk.gov.hmrc.iossreturns.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckOwnIossNumberFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockPreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockIntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]
  private val registration = arbitraryEtmpRegistration.arbitrary.sample.value

  class Harness(iossNumber: String, previousRegistrationService: PreviousRegistrationService, intermediaryRegistrationConnector: IntermediaryRegistrationConnector) extends
    CheckOwnIossNumberFilterImpl(iossNumber, previousRegistrationService, intermediaryRegistrationConnector) {
    def callFilter(request: AuthorisedRequest[_]): Future[Option[Result]] = filter(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)
  }

  def intermediaryRegistrationWithClients(iossNumber: Seq[String]): IntermediaryRegistrationWrapper = {
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
      etmpDisplayRegistration = arbitraryIntermediaryDisplayRegistration.arbitrary.sample.value.copy(
        clientDetails = iossNumber.map { ioss =>
          arbitraryEtmpClientDetails.arbitrary.sample.value.copy(clientIossID = ioss)
        }
      )
    )
  }

  def enrolmentsWithIntermediaries(intermediaryNumbers: Seq[String]): Enrolments = {
    Enrolments(intermediaryNumbers.map { int =>
      Enrolment(
        key = "HMRC-IOSS-INT",
        identifiers = Seq(EnrolmentIdentifier("IntNumber", int)),
        state = "Activated"
      )}.toSet
    )
  }

  def buildRegistrationRequest(intermediaryNumber: Option[String], iossNumber: String, enrolments: Enrolments): AuthorisedRequest[_] = {
    AuthorisedRequest(FakeRequest(), userAnswersId, testCredentials.providerId, vrn, iossNumber, registration, intermediaryNumber, enrolments)
  }

  ".filter" - {

    "must return None" - {

      "when the ioss number exists" in {

        val application = applicationBuilder(None).build()

        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn
          List(PreviousRegistration(iossNumber = iossNumber, startPeriod = period, endPeriod = period)).toFuture

        running(application) {
          val request = buildRegistrationRequest(
            intermediaryNumber = None,
            iossNumber = iossNumber,
            enrolments = enrolments
          )
          val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "when an active intermediary has access to an IOSS client" in {

        val currentIntermediary = "IN9001234567"

        val application = applicationBuilder(None).build()

        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn
          List(PreviousRegistration(iossNumber = iossNumber, startPeriod = period, endPeriod = period)).toFuture
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture

        running(application) {
          val request = buildRegistrationRequest(
            intermediaryNumber = Some(currentIntermediary),
            iossNumber = iossNumber,
            enrolments = enrolments
          )

          val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "when intermediary has access via previous enrolments" in {

        val currentIntermediary = "IN9001234567"
        val previousIntermediary = "IN9001234568"

        val application = applicationBuilder(None).build()

        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn
          List(PreviousRegistration(iossNumber = iossNumber, startPeriod = period, endPeriod = period)).toFuture
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Nil).toFuture
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture

        running(application) {
          val request = buildRegistrationRequest(
            intermediaryNumber = Some(previousIntermediary),
            iossNumber = iossNumber,
            enrolments = enrolmentsWithIntermediaries(Seq(currentIntermediary, previousIntermediary))
          )

          val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "when an IOSS client is found in previous registrations" in {

        val application = applicationBuilder(None).build()

        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn
          List(PreviousRegistration(iossNumber = iossNumber, startPeriod = period, endPeriod = period)).toFuture

        running(application) {
          val request = buildRegistrationRequest(
            intermediaryNumber = None,
            iossNumber = iossNumber,
            enrolments = enrolments
          )

          val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }
    }

    "must return Unauthorised" - {

      "when intermediary has no access to any of the clients" in {

        val currentIntermediary = "IN9001234567"

        val application = applicationBuilder(None).build()

        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Nil).toFuture

        running(application) {
          val request = buildRegistrationRequest(
            intermediaryNumber = Some(currentIntermediary),
            iossNumber = iossNumber,
            enrolments = enrolmentsWithIntermediaries(Seq(currentIntermediary))
          )

          val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Unauthorized
        }

      }

      "when an ioss registration does not exist" in {

        val application = applicationBuilder(None).build()

        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn Nil.toFuture

        running(application) {
          val request = buildRegistrationRequest(
            intermediaryNumber = None,
            iossNumber = "",
            enrolments = enrolments
          )

          val controller = new Harness(iossNumber, mockPreviousRegistrationService, mockIntermediaryRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Unauthorized
        }
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
