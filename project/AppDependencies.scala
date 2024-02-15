import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "domain-play-30"             % "9.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % hmrcMongoVersion,
    "org.scalatest"           %% "scalatest"                % "3.2.15",
    "org.playframework"       %% "play-test"                % PlayVersion.current,
    "com.vladsch.flexmark"    % "flexmark-all"              % "0.64.6",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0",
    "org.scalatestplus"       %% "scalacheck-1-15"          % "3.2.11.0",
    "org.scalatestplus"       %% "mockito-4-6"              % "3.2.15.0",
    "org.mockito"             %% "mockito-scala"            % "1.17.30"
  ).map(_ % "test, it")
}
