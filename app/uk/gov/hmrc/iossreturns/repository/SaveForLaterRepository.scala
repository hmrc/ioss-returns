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

package uk.gov.hmrc.iossreturns.repository

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.ObservableFuture
import play.api.libs.json.Format
import uk.gov.hmrc.iossreturns.config.AppConfig
import uk.gov.hmrc.iossreturns.crypto.SavedUserAnswersEncryptor
import uk.gov.hmrc.iossreturns.logging.Logging
import uk.gov.hmrc.iossreturns.models._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SaveForLaterRepository @Inject()(
                                        val mongoComponent: MongoComponent,
                                        encryptor: SavedUserAnswersEncryptor,
                                        appConfig: AppConfig
                                      )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[EncryptedSavedUserAnswers](
    collectionName = "saved-user-answers",
    mongoComponent = mongoComponent,
    domainFormat = EncryptedSavedUserAnswers.format,
    replaceIndexes = true,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.DAYS)
      ),
      IndexModel(
        Indexes.ascending("iossNumber", "period"),
        IndexOptions()
          .name("userAnswersReferenceIndex")
          .unique(true)
      )
    )
  ) with Logging {

  import uk.gov.hmrc.mongo.play.json.Codecs.toBson

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byIossNumberAndPeriod(iossNumber: String, period: Period): Bson =
    Filters.and(
      Filters.equal("iossNumber", iossNumber),
      Filters.equal("period", period.toBson)
    )

  def set(savedUserAnswers: SavedUserAnswers): Future[SavedUserAnswers] = {

    val encryptedAnswers = encryptor.encryptAnswers(savedUserAnswers, savedUserAnswers.iossNumber)

    collection
      .replaceOne(
        filter = byIossNumberAndPeriod(savedUserAnswers.iossNumber, savedUserAnswers.period),
        replacement = encryptedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => savedUserAnswers)
  }

  def get(iossNumber: String): Future[Seq[SavedUserAnswers]] =
    collection
      .find(Filters.equal("iossNumber", toBson(iossNumber)))
      .toFuture()
      .map(_.map {
        case n: NewEncryptedSavedUserAnswers =>
          encryptor.decryptAnswers(n, n.iossNumber)
        case l: LegacyEncryptedSavedUserAnswers =>
          encryptor.decryptLegacyAnswers(l, l.iossNumber)
      })

  def get(iossNumber: String, period: Period): Future[Option[SavedUserAnswers]] =
    collection
      .find(
        Filters.and(
          Filters.equal("iossNumber", toBson(iossNumber)),
          Filters.equal("period", toBson(period))
        )
      ).headOption()
      .map(_.map {
        case n: NewEncryptedSavedUserAnswers =>
          encryptor.decryptAnswers(n, n.iossNumber)
        case l: LegacyEncryptedSavedUserAnswers =>
          encryptor.decryptLegacyAnswers(l, l.iossNumber)
      })

  def clear(iossNumber: String, period: Period): Future[Boolean] =
    collection
      .deleteOne(byIossNumberAndPeriod(iossNumber, period))
      .toFuture()
      .map(_ => true)
}
