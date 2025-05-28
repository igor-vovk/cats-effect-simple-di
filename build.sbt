import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / scalaVersion := "3.3.6"

ThisBuild / organization := "io.github.igor-vovk"

ThisBuild / homepage := Some(url("https://github.com/igor-vovk/cats-effect-simple-di"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer(
    "igor-vovk",
    "Ihor Vovk",
    "ideals-03.gushing@icloud.com",
    url("https://ivovk.me"),
  )
)
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

lazy val noPublish = List(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false,
  publish / skip  := true,
)

lazy val catsEffectSimpleDiCore = (project in file("."))
  .settings(
    moduleName := "cats-effect-simple-di",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect"    % "3.6.1",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.1",
      "org.scalatest" %% "scalatest"      % "3.2.19" % Test,
    ),
  )

lazy val root = (project)
  .aggregate(
    catsEffectSimpleDiCore
  )
  .settings(
    noPublish
  )
