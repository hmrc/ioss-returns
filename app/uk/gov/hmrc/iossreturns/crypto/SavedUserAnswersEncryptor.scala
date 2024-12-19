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

import play.api.libs.json.Json
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.models.{LegacyEncryptedSavedUserAnswers, NewEncryptedSavedUserAnswers, SavedUserAnswers}
import uk.gov.hmrc.iossreturns.services.crypto.EncryptionService

import javax.inject.Inject

class SavedUserAnswersEncryptor @Inject()(
                                           appConfig: AppConfig,
                                           encryptionService: EncryptionService,
                                           secureGCMCipher: AesGCMCrypto
                                         ) {

  protected val encryptionKey: String = appConfig.encryptionKey

  def encryptAnswers(answers: SavedUserAnswers, iossNumber: String): NewEncryptedSavedUserAnswers = {
    def encryptAnswerValue(answerValue: String): String = encryptionService.encryptField(answerValue)

    NewEncryptedSavedUserAnswers(
      iossNumber = iossNumber,
      period = answers.period,
      data = encryptAnswerValue(answers.data.toString),
      lastUpdated = answers.lastUpdated
    )
  }

  def decryptAnswers(encryptedAnswers: NewEncryptedSavedUserAnswers, iossNumber: String): SavedUserAnswers = {
    def decryptAnswerValue(answerValue: String): String = encryptionService.decryptField(answerValue)

    SavedUserAnswers(
      iossNumber = iossNumber,
      period = encryptedAnswers.period,
      data = Json.parse(decryptAnswerValue(encryptedAnswers.data)),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }

  def decryptLegacyAnswers(encryptedAnswers: LegacyEncryptedSavedUserAnswers, iossNumber: String): SavedUserAnswers = {
    def decryptAnswerValue(answerValue: EncryptedValue): String = secureGCMCipher.decrypt(answerValue, iossNumber, encryptionKey)

    SavedUserAnswers(
      iossNumber = iossNumber,
      period = encryptedAnswers.period,
      data = Json.parse(decryptAnswerValue(encryptedAnswers.data)),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }
}
