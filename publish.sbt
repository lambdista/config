ThisBuild / organization := "com.lambdista.config"
ThisBuild / organizationName := "lambdista"
ThisBuild / organizationHomepage := Some(url("https://www.alessandrolacava.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/lambdista/config"),
    "scm:git@github.com:lambdista/config.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "lambdista",
    name  = "Alessandro Lacava",
    email = "alessandrolacava@gmail.com",
    url   = url("https://www.alessandrolacava.com/")
  )
)

ThisBuild / description := "A type safe, purely functional configuration library for Scala."
ThisBuild / homepage := Some(url("https://github.com/lambdista/config"))

ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / publishMavenStyle := true

ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

ThisBuild / bintrayReleaseOnPublish := true

ThisBuild / bintrayPackageLabels := Seq("configuration", "purely functional", "type safe", "scala")

