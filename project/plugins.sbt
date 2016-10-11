logLevel := Level.Warn

lazy val scalafmtVersion = "0.4.5"

lazy val tutVersion = "0.4.4"

lazy val sbtUpdatesVersion = "0.2.0"

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % scalafmtVersion)

addSbtPlugin("org.tpolecat" % "tut-plugin" % tutVersion)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % sbtUpdatesVersion)
