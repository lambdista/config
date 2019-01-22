import Dependencies._

lazy val projectName = "config"

lazy val projectScalaVersion = "2.12.8"

lazy val commonSettings = Seq(
  moduleName := projectName,
  organization := "com.lambdista",
  scalaVersion := projectScalaVersion,
  version := "0.5.4",
  crossScalaVersions := Seq(projectScalaVersion, "2.11.12"),
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
  scalacOptions := Seq(
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-Ypartial-unification",
    "-encoding",
    "utf8",
    "-deprecation",
    "-unchecked",
    "-Ywarn-unused-import",
    "-Ywarn-unused",
    "-Ywarn-dead-code",
    "-Yno-adapted-args"
  ),
  scalafmtConfig := Some(file(".scalafmt.conf")),
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
  .aggregate(core, util, typesafe)
  .dependsOn(core, util, typesafe)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := s"$projectName-root",
    (unmanagedSourceDirectories in Compile) := Nil,
    (unmanagedSourceDirectories in Test) := Nil
  )

lazy val core = (project in file("core"))
  .dependsOn(util)
  .settings(commonSettings)
  .settings(moduleName := projectName, libraryDependencies ++= coreDeps, dependencyOverrides += shapeless)

lazy val util = (project in file("util"))
  .settings(commonSettings)
  .settings(moduleName := s"$projectName-util")

lazy val typesafe = (project in file("typesafe"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(moduleName := s"$projectName-typesafe", libraryDependencies ++= typesafeDeps)

lazy val docs = (project in file("docs"))
  .dependsOn(core, typesafe)
  .enablePlugins(TutPlugin)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := s"$projectName-docs",
    tutSourceDirectory := file("docs/src/tut"),
    tutTargetDirectory := file(".")
  )
