import java.time.LocalDate

import Dependencies._

lazy val projectName = "config"

lazy val commonSettings = Seq(
  moduleName := projectName,
  organization := "com.lambdista",
  scalaVersion := projectScalaVersion,
  version := "0.7.0",
  crossScalaVersions := Seq(projectScalaVersion, "2.12.10", "2.11.12"),
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
  scalacOptions :=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) =>
        Seq(
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
        )
      case _ =>
        Seq(
          "-feature",
          "-language:higherKinds",
          "-language:implicitConversions",
          "-language:postfixOps",
          "-language:experimental.macros",
          "-Ypartial-unification",
          "-encoding",
          "utf8",
          "-deprecation",
          "-unchecked",
          "-Ywarn-unused-import",
          "-Ywarn-unused",
          "-Ywarn-dead-code",
          "-Yno-adapted-args"
        )
    }),
  scalafmtOnCompile := true,
  initialCommands in console :=
    """
      |import scala.util._
      |import scala.concurrent.duration._
      |import scala.concurrent.duration.Duration._
      |import com.lambdista.config._
    """.stripMargin
)

lazy val noPublishSettings = Seq(skip in publish := true)

lazy val config = (project in file("."))
  .aggregate(core, typesafe)
  .dependsOn(core, typesafe)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := s"$projectName-root",
    (unmanagedSourceDirectories in Compile) := Nil,
    (unmanagedSourceDirectories in Test) := Nil
  )

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    moduleName := projectName,
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) | Some((2, 12)) => coreDeps
      case _                             => coreDeps2_11
    })
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
