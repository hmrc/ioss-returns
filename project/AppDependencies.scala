import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "7.23.0"
  private val hmrcMongoVersion = "1.4.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
    "uk.gov.hmrc" %% "domain" % "8.1.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion,
    "org.scalatest" %% "scalatest" % "3.2.12",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0",
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0",
    "org.mockito" %% "mockito-scala" % "1.17.7",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2"
  ).map(_ % "test, it")
}
