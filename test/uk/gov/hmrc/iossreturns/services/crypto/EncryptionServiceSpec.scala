package uk.gov.hmrc.iossreturns.services.crypto

import play.api.Configuration
import play.api.test.Helpers.running
import uk.gov.hmrc.iossreturns.base.SpecBase

class EncryptionServiceSpec extends SpecBase {

  "EncryptionService" - {

    "must encrypt text value" in {

      val textToEncrypt: String = "Test String"

      val application = applicationBuilder().build()

      running(application) {

        val configuration: Configuration = application.configuration

        val service: EncryptionService = EncryptionService(configuration)

        val result = service.encryptField(textToEncrypt)

        result mustBe a[String]
      }
    }

    "must decrypt text value" in {

      val textToEncrypt: String = "Test String"

      val application = applicationBuilder().build()

      running(application) {

        val configuration: Configuration = application.configuration

        val service: EncryptionService = EncryptionService(configuration)

        val encryptedValue = service.encryptField(textToEncrypt)

        val result = service.decryptField(encryptedValue)

        result mustBe a[String]
      }
    }

    "must throw a Security Exception if text value can't be decrypted" in {

      val textToEncrypt: String = "Test String"

      val application = applicationBuilder().build()

      running(application) {

        val configuration: Configuration = application.configuration

        val service: EncryptionService = EncryptionService(configuration)

        val invalidEncryptedValue = service.encryptField(textToEncrypt) + "any"

        val result = intercept[SecurityException](service.decryptField(invalidEncryptedValue))
        result.getMessage mustBe "Unable to decrypt value"
      }
    }
  }
}
