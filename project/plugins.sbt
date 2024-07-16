resolvers += "OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12+38-6b30fb12-SNAPSHOT")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.11.0")
