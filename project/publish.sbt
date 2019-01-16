ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

lazy val projectUrl    = "https://github.com/lambdista/config"
lazy val developerId   = "lambdista"
lazy val developerName = "Alessandro Lacava"
lazy val developerUrl  = "http://www.alessandrolacava.com"
lazy val licenseName   = "Apache License, Version 2.0"
lazy val licenseUrl    = "http://www.apache.org/licenses/LICENSE-2.0.txt"
lazy val scmUrl        = projectUrl
lazy val scmConnection = "scm:git:" + scmUrl

pomExtra :=
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
    <scm>
      <url>{scmUrl}</url>
      <connection>{scmConnection}</connection>
    </scm>
      <developers>
        <developer>
          <id>{developerId}</id>
          <name>{developerName}</name>
          <url>{developerUrl}</url>
        </developer>
      </developers>
    <url>{projectUrl}</url>

ThisBuild / publishMavenStyle := true

publishConfiguration := publishConfiguration.value.withOverwrite(true)
