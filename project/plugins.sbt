lazy val mdocVersion       = "2.2.21"
lazy val scovVersion       = "1.8.2"
lazy val ciReleaseVersion  = "1.5.7"
lazy val sbtUpdatesVersion = "0.5.3"
lazy val revolverVersion   = "0.9.1"
lazy val scalafmtVersion   = "2.4.3"

addSbtPlugin("org.scalameta"    % "sbt-scalafmt"   % scalafmtVersion)
addSbtPlugin("com.timushev.sbt" % "sbt-updates"    % sbtUpdatesVersion)
addSbtPlugin("io.spray"         % "sbt-revolver"   % revolverVersion)
addSbtPlugin("org.scalameta"    % "sbt-mdoc"       % mdocVersion)
addSbtPlugin("org.scoverage"    % "sbt-scoverage"  % scovVersion)
addSbtPlugin("com.geirsson"     % "sbt-ci-release" % ciReleaseVersion)
