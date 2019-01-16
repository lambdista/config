resolvers += Resolver.url(
  "rtimush/sbt-plugin-snapshots",
  new URL("https://dl.bintray.com/rtimush/sbt-plugin-snapshots/")
)(Resolver.ivyStylePatterns)
resolvers += "Typesafe repository" at "https://dl.bintray.com/typesafe/maven-releases/"

classpathTypes += "maven-plugin"

lazy val scalafmtVersion       = "1.4.0"
lazy val sbtUpdatesVersion     = "0.3.3"
lazy val buildInfoVersion      = "0.7.0"
lazy val coursierVersion       = "1.0.0"
lazy val nativePackagerVersion = "1.3.2"
lazy val revolverVersion       = "0.9.1"
lazy val tutVersion            = "0.6.10"
lazy val pgpVersion            = "1.1.1"
lazy val sonatypeVersion       = "2.3"

addSbtPlugin("io.get-coursier"  % "sbt-coursier"        % coursierVersion)
addSbtPlugin("com.geirsson"     % "sbt-scalafmt"        % scalafmtVersion)
addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % sbtUpdatesVersion)
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo"       % buildInfoVersion)
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % nativePackagerVersion)
addSbtPlugin("io.spray"         % "sbt-revolver"        % revolverVersion)
addSbtPlugin("org.tpolecat"     % "tut-plugin"          % tutVersion)
addSbtPlugin("com.jsuereth"     % "sbt-pgp"             % pgpVersion)
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"        % sonatypeVersion)
