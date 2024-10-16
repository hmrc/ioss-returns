package uk.gov.hmrc.iossreturns.repository

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.crypto.{SavedUserAnswersEncryptor, SecureGCMCipher}
import uk.gov.hmrc.iossreturns.generators.Generators
import uk.gov.hmrc.iossreturns.models._
import uk.gov.hmrc.iossreturns.services.crypto.EncryptionService
import uk.gov.hmrc.iossreturns.utils.StringUtils
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SaveForLaterRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedSavedUserAnswers]
    with CleanMongoCollectionSupport
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with Generators {

  private val appConfig = mock[AppConfig]
  private val mockSecureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private val mockEncryptionService: EncryptionService = mock[EncryptionService]
  private val encryptor = new SavedUserAnswersEncryptor(appConfig, mockEncryptionService, mockSecureGCMCipher)

  private val instant = Instant.now
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  override protected val repository =
    new SaveForLaterRepository(
      mongoComponent = mongoComponent,
      encryptor = encryptor,
      appConfig = appConfig
    )

  when(mockEncryptionService.encryptField(any())) thenReturn "encryptedText"
  when(mockEncryptionService.decryptField(any())) thenReturn arbitrarySavedUserAnswers.arbitrary.sample.value.data.toString()

  ".set savedAnswers" - {

    "must insert returns for the same IOSS Number but different periods" in {

      val answers = arbitrary[SavedUserAnswers].sample.value
      val answers1 = answers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val answers2Period = StandardPeriod(year = answers1.period.year + 1, month = answers1.period.month)
      val answers2 = answers1.copy(
        period = answers2Period,
        lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS)
      )

      val insertResult1 = repository.set(answers1).futureValue
      val insertReturn2 = repository.set(answers2).futureValue
      val databaseRecords = findAll().futureValue
      val decryptedDatabaseRecords =
        databaseRecords.map(e => determineEncryptionType(e))

      insertResult1 mustBe answers1
      insertReturn2 mustBe answers2
      decryptedDatabaseRecords must contain theSameElementsAs Seq(answers1, answers2)
    }

    "must insert saved answers for different IOSS Numbers in the same period" in {
      val answers = arbitrary[SavedUserAnswers].sample.value
      val answers1 = answers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val iossNumber2 = StringUtils.rotateDigitsInString(answers1.iossNumber).mkString
      val answers2 = answers1.copy(
        iossNumber = iossNumber2,
        lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS)
      )

      val insertResult1 = repository.set(answers1).futureValue
      val insertReturn2 = repository.set(answers2).futureValue
      val databaseRecords = findAll().futureValue
      val decryptedDatabaseRecords =
        databaseRecords.map(e => determineEncryptionType(e))

      insertResult1 mustBe answers1
      insertReturn2 mustBe answers2
      decryptedDatabaseRecords must contain theSameElementsAs Seq(answers1, answers2)
    }

    "must replace saved answers with the same IOSS Number and period" in {

      val answers = arbitrary[SavedUserAnswers].sample.value
      val answers2 = answers.copy(lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val insertResult1 = repository.set(answers).futureValue
      val insertResult2 = repository.set(answers2).futureValue

      insertResult1 mustBe answers
      insertResult2 mustBe answers2

      val decryptedDatabaseRecords =
        findAll().futureValue.map(e => determineEncryptionType(e))

      decryptedDatabaseRecords must contain only answers2
    }
  }

  ".get many" - {

    "must return all records for the given IOSS Number" in {

      val answers = arbitrary[SavedUserAnswers].sample.value
      val answers1 = answers.copy(lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val answers2Period = StandardPeriod(year = answers1.period.year + 1, month = answers1.period.month)
      val answers2 = answers1.copy(
        period = answers2Period,
        lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS)
      )

      val iossNumber3 = StringUtils.rotateDigitsInString(answers1.iossNumber).mkString
      val answers3 = answers1 copy (
        iossNumber = iossNumber3
        )

      insert(encryptor.encryptAnswers(answers1, answers1.iossNumber)).futureValue
      insert(encryptor.encryptAnswers(answers2, answers2.iossNumber)).futureValue
      insert(encryptor.encryptAnswers(answers3, answers3.iossNumber)).futureValue

      val returns = repository.get(answers1.iossNumber).futureValue

      returns must contain theSameElementsAs Seq(answers1, answers2)
    }
  }

  ".get one" - {

    "must return Saved answers record when one exists for this IOSS Number and period" in {

      val answers1 = arbitrary[SavedUserAnswers].sample.value

      val answers = answers1.copy(lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))

      insert(encryptor.encryptAnswers(answers, answers.iossNumber)).futureValue

      val result = repository.get(answers.iossNumber, answers.period).futureValue

      result.value mustEqual answers
    }

    "must return None when a return does not exist for this IOSS Number and period" in {

      val iossNumber = arbitrary[String].sample.value
      val period = arbitrary[Period].sample.value

      val result = repository.get(iossNumber, period).futureValue

      result must not be defined
    }
  }

  ".clear" - {

    "must return true when Saved Answers Record is deleted" in {

      val answers = arbitrary[SavedUserAnswers].sample.value

      insert(encryptor.encryptAnswers(answers, answers.iossNumber)).futureValue

      val result = repository.clear(answers.iossNumber, answers.period).futureValue

      result mustEqual true
    }
  }

  private def determineEncryptionType(answers: EncryptedSavedUserAnswers): SavedUserAnswers = {
    answers match {
      case l: LegacyEncryptedSavedUserAnswers =>
        encryptor.decryptLegacyAnswers(l, l.iossNumber)
      case n: NewEncryptedSavedUserAnswers =>
        encryptor.decryptAnswers(n, n.iossNumber)
      case _ => throw new IllegalArgumentException("Not a valid EncryptedSavedUserAnswers type.")
    }
  }
}
