import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / scalaVersion := "3.3.6"

ThisBuild / organization := "me.ivovk"

ThisBuild / homepage   := Some(url("https://github.com/igor-vovk/cedi"))
ThisBuild / licenses   := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer(
    "igor-vovk",
    "Ihor Vovk",
    "ideals-03.gushing@icloud.com",
    url("https://ivovk.me"),
  )
)

ThisBuild / tpolecatExcludeOptions ++= Set(
  ScalacOptions.warnNonUnitStatement,
  ScalacOptions.warnValueDiscard,
)


lazy val noPublish = List(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false,
  publish / skip  := true,
)

lazy val cedi = (project in file("."))
  .settings(
    moduleName := "cedi",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect"    % "3.6.1",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.1",
      "org.scalatest" %% "scalatest"      % "3.2.19" % Test,
    ),
  )

lazy val root = (project)
  .aggregate(
    cedi
  )
  .settings(
    noPublish
  )
