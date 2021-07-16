import java.time.LocalDate

import Dependencies._

lazy val projectName = "config"

lazy val commonSettings = Seq(
  moduleName := projectName,
  organization := "com.lambdista",
  description := "A type safe, purely functional configuration library for Scala.",
  homepage := Some(url("https://github.com/lambdista/config")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "lambdista",
      "Alessandro Lacava",
      "alessandrolacava@gmail.com",
      url("https://alessandrolacava.com")
    )
  ),
  version := "0.8.0",
  scalaVersion := projectScalaVersion,
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
  scalacOptions := Seq(
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:experimental.macros",
    "-encoding",
    "utf8",
    "-deprecation",
    "-unchecked",
    "-Ywarn-unused",
    "-Ywarn-dead-code"
  ),
  scalafmtOnCompile := true,
  console / initialCommands :=
    """
      |import scala.util._
      |import scala.concurrent.duration._
      |import scala.concurrent.duration.Duration._
      |import com.lambdista.config._
    """.stripMargin,
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val noPublishSettings = Seq(publish / skip := true)

lazy val config = (project in file("."))
  .aggregate(core, typesafe)
  .dependsOn(core, typesafe)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := s"$projectName-root",
    (Compile / unmanagedSourceDirectories) := Nil,
    (Test / unmanagedSourceDirectories) := Nil
  )

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    moduleName := projectName,
    libraryDependencies ++= coreDeps
  )

lazy val typesafe = (project in file("typesafe"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(moduleName := s"$projectName-typesafe", libraryDependencies ++= typesafeDeps)

lazy val docs = (project in file("config-docs"))
  .dependsOn(core, typesafe)
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := s"$projectName-docs",
    mdocIn := file("config-docs/src/mdoc"),
    mdocOut := file("."),
    mdocVariables := Map(
      "YEAR" -> LocalDate.now.getYear.toString
    )
  )
