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

package uk.gov.hmrc.iossreturns.controllers

import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.iossreturns.base.SpecBase
import uk.gov.hmrc.iossreturns.models.fileUpload.{FileUploadOutcome, UploadDocument}
import uk.gov.hmrc.iossreturns.repository.UploadRepository

import scala.concurrent.{ExecutionContext, Future}

class FileUploadOutcomeResponseControllerSpec extends SpecBase with ScalaCheckPropertyChecks {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "FileUploadOutcomeResponseController" - {

    "return 200 and the file name if upload exists" in {

      val mockRepo = mock[UploadRepository]
      val reference = "abc123"
      val doc = UploadDocument(
        _id = reference,
        status = "UPLOADED",
        fileName = Some("test.csv"),
        checksum = Some("abc123"),
        size = Some(1024L)
      )
      when(mockRepo.getUpload(reference)).thenReturn(Future.successful(Some(doc)))

      val app = applicationBuilder()
        .overrides(bind[UploadRepository].toInstance(mockRepo))
        .build()

      running(app) {

        val request = FakeRequest(GET, routes.FileUploadOutcomeResponseController.get(reference).url)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(FileUploadOutcome(Some("test.csv"), "UPLOADED", None))

      }
    }

    "return 404 if no upload exists for the reference" in {

      val mockRepo = mock[UploadRepository]
      val reference = "abc123"

      when(mockRepo.getUpload(reference)).thenReturn(Future.successful(None))

      val app = applicationBuilder()
        .overrides(bind[UploadRepository].toInstance(mockRepo))
        .build()

      running(app) {

        val request = FakeRequest(GET, routes.FileUploadOutcomeResponseController.get(reference).url)
        val result = route(app, request).value

        status(result) mustEqual NOT_FOUND
        (contentAsJson(result) \ "error").as[String] must include("No upload found")

      }
    }
  }
}
