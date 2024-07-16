scalaVersion := "3.3.3"
scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-Xfatal-warnings", "-Wunused:imports")

organization := "io.github.igor-vovk"
name := "cats-effect-simple-di"
versionScheme := Some("early-semver")

homepage := Some(url("https://github.com/igor-vovk/cats-effect-simple-di"))
licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "igor-vovk",
    "Ihor Vovk",
    "ideals-03.gushing@icloud.com",
    url("https://ivovk.me")
  )
)


githubOwner := "igor-vovk"
githubRepository := "cats-effect-simple-di"

lazy val Versions = new {
  val catsEffect = "3.5.4"
  val scalatest  = "3.2.18"
}

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % Versions.catsEffect % "provided",

      "org.typelevel" %% "log4cats-slf4j" % "2.7.0" % "provided",
      "ch.qos.logback" % "logback-classic" % "1.5.6" % "provided",

      "org.scalatest" %% "scalatest" % Versions.scalatest % Test,
    )
  )
