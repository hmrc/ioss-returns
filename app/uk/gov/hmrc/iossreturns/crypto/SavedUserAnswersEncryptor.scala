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

import javax.inject.Inject

class SavedUserAnswersEncryptor @Inject()(
                                 crypto: SecureGCMCipher
                               ) {

  def encryptData(data: JsValue, iossNumber: String, key: String): EncryptedValue = {
    def e(field: String): EncryptedValue = crypto.encrypt(field, iossNumber, key)

    e(data.toString)
  }

  def decryptData(data: EncryptedValue, iossNumber: String, key: String): JsValue = {
    def d(field: EncryptedValue): String = crypto.decrypt(field, iossNumber, key)
    Json.parse(d(data))

  }

  def encryptAnswers(answers: SavedUserAnswers, iossNumber: String, key: String): EncryptedSavedUserAnswers = {
    EncryptedSavedUserAnswers(
      iossNumber = iossNumber,
      period = answers.period,
      data = encryptData(answers.data, iossNumber, key),
      lastUpdated = answers.lastUpdated
    )
  }

  def decryptAnswers(encryptedAnswers: EncryptedSavedUserAnswers, iossNumber: String, key: String): SavedUserAnswers = {
    SavedUserAnswers(
      iossNumber = iossNumber,
      period = encryptedAnswers.period,
      data = decryptData(encryptedAnswers.data, iossNumber, key),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }
}
