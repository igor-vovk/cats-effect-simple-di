import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / scalaVersion := "3.3.3"
ThisBuild / scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-Xfatal-warnings", "-Wunused:imports")

name := "cats-effect-simple-di"

ThisBuild / organization := "io.github.igor-vovk"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / homepage := Some(url("https://github.com/igor-vovk/cats-effect-simple-di"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer(
    "igor-vovk",
    "Ihor Vovk",
    "ideals-03.gushing@icloud.com",
    url("https://ivovk.me")
  )
)
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
sonatypeRepository := sonatypeCentralHost


libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4" % "provided",

  "org.typelevel" %% "log4cats-slf4j" % "2.7.0" % "provided",
  "ch.qos.logback" % "logback-classic" % "1.5.6" % "provided",

  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
)
