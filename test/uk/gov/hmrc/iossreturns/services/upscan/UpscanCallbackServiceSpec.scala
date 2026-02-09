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

package uk.gov.hmrc.iossreturns.services.upscan

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.fileUpload.*
import uk.gov.hmrc.iossreturns.repository.UploadRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class UpscanCallbackServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "UpscanCallbackService" - {

    "mark upload as uploaded on success callback" in {

      val mockRepo = mock[UploadRepository]
      when(mockRepo.markAsUploaded(any(), any(), any(), any())).thenReturn(Future.successful(()))

      val service = new UpscanCallbackService(mockRepo)

      val callback = UpscanCallbackSuccess(
        reference = "123",
        fileStatus = "READY",
        uploadDetails = UploadDetails(
          fileName = "test.csv",
          fileMimeType = "text/csv",
          uploadTimestamp = Instant.now(),
          checksum = "abc123",
          size = 1024
        )
      )

      whenReady(service.handleUpscanCallback(callback)) { _ =>
        verify(mockRepo).markAsUploaded("123", "abc123", "test.csv", 1024)
      }
    }

    "mark upload as failed on failure callback" in {
      val mockRepo = mock[UploadRepository]
      when(mockRepo.markAsFailed(any(), any()))
        .thenReturn(Future.successful(()))

      val service = new UpscanCallbackService(mockRepo)

      val callback = UpscanCallbackFailure(
        reference = "123",
        fileStatus = "FAILED",
        failureDetails = FailureDetails("QUARANTINE")
      )

      whenReady(service.handleUpscanCallback(callback)) { _ =>
        verify(mockRepo).markAsFailed("123", "QUARANTINE")
      }
    }
  }

}
