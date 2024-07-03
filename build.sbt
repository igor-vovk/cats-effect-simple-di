
organization := "com.ihorvovk"
name := "cats-effect-simple-di"
version := "0.0.1"

scalaVersion := "3.4.2"
scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-Xfatal-warnings", "-Wunused:imports")

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
