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

package uk.gov.hmrc.iossreturns.crypto

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.iossreturns.models.{EncryptedSavedUserAnswers, SavedUserAnswers}
import uk.gov.hmrc.iossreturns.services.crypto.EncryptionService

import javax.inject.Inject

class SavedUserAnswersEncryptor @Inject()(
                                           encryptionService: EncryptionService
                                         ) {

  def encryptData(data: JsValue): String = {
    def encryptAnswerValue(field: String): String = encryptionService.encryptField(field)

    encryptAnswerValue(data.toString)
  }

  def decryptData(data: String): JsValue = {
    def decryptAnswerValue(field: String): String = encryptionService.decryptField(field)

    Json.parse(decryptAnswerValue(data))

  }

  def encryptAnswers(answers: SavedUserAnswers, iossNumber: String): EncryptedSavedUserAnswers = {
    EncryptedSavedUserAnswers(
      iossNumber = iossNumber,
      period = answers.period,
      data = encryptData(answers.data),
      lastUpdated = answers.lastUpdated
    )
  }

  def decryptAnswers(encryptedAnswers: EncryptedSavedUserAnswers, iossNumber: String): SavedUserAnswers = {
    SavedUserAnswers(
      iossNumber = iossNumber,
      period = encryptedAnswers.period,
      data = decryptData(encryptedAnswers.data),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }
}
