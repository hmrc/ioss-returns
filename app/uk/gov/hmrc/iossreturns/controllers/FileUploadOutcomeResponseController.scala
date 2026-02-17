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

import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.iossreturns.controllers.actions.DefaultAuthenticatedControllerComponents
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models.fileUpload.FileUploadOutcome
import uk.gov.hmrc.iossreturns.repository.UploadRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FileUploadOutcomeResponseController @Inject()(
                                                     uploadRepository: UploadRepository,
                                                     cc: DefaultAuthenticatedControllerComponents
                                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def get(reference: String): Action[AnyContent] = Action.async {
    implicit request =>
    uploadRepository.getUpload(reference).map {
      case Some(doc) =>
        Ok(Json.toJson(
          FileUploadOutcome(
            fileName = doc.fileName,
            status = doc.status,
            failureReason = doc.failureReason.map(_.asString)
          )
        ))
      case None =>
        NotFound(Json.obj("error" -> s"No upload found for reference $reference"))
    }
  }
}
