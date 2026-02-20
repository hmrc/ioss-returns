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

import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models.fileUpload.{FailureReason, UpscanCallbackFailure, UpscanCallbackRequest, UpscanCallbackSuccess, UpscanCallbackUploading}
import uk.gov.hmrc.iossreturns.repository.UploadRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class UpscanCallbackService @Inject()(uploadRepository: UploadRepository)(implicit ec: ExecutionContext) extends Logging {

  private val maxFileSize: Long = 2 * 1024 * 1024 // 2MB

  def handleUpscanCallback(callback: UpscanCallbackRequest): Future[Unit] =
    callback match {

      case success: UpscanCallbackSuccess =>
        val fileName = success.uploadDetails.fileName.toLowerCase
        val fileType = success.uploadDetails.fileMimeType
        val isCsv = fileName.endsWith(".csv") && fileType == "text/csv"
        val failureReasonOption: Option[FailureReason] = {
          if (fileName.endsWith(".ods")) {
            Some(FailureReason.InvalidFileType)
          } else if (!isCsv) {
            Some(FailureReason.NotCSV)
          } else if (success.uploadDetails.size > maxFileSize) {
            Some(FailureReason.TooLarge)
          } else {
            None
          }
        }

        failureReasonOption match {
          case Some(reason) =>
            logger.warn(s"Invalid upload for reference ${success.reference}: ${reason.asString}")
            uploadRepository.markAsFailed(
              reference = success.reference,
              reason = reason,
              fileName = Some(success.uploadDetails.fileName)
            )
          case None =>
            logger.info(s"Upscan SUCCESS for reference ${success.reference}")
            uploadRepository.markAsUploaded(
              reference = success.reference,
              checksum = success.uploadDetails.checksum,
              fileName = success.uploadDetails.fileName,
              size = success.uploadDetails.size
            )
        }


      case failure: UpscanCallbackFailure =>
        logger.warn(s"Upscan FAILURE for reference ${failure.reference}, reason=${failure.failureDetails.failureReason.asString}")
        uploadRepository.markAsFailed(
          reference = failure.reference,
          reason = failure.failureDetails.failureReason,
          fileName = failure.uploadDetails.map(_.fileName)
        )

      case uploading: UpscanCallbackUploading =>
        logger.info(s"File is still uploading for reference ${uploading.reference}")
        Future.successful(())
    }
}
