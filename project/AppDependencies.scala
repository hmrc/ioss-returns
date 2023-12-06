import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.23.0"
  private val hmrcMongoVersion = "1.4.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain" % "8.1.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % "test, it",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.17" % "test",
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % "test",
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0",
    "org.mockito" %% "mockito-scala" % "1.17.7",
  )
}
