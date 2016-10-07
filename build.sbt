import Dependencies._

lazy val projectName = "config"

lazy val commonSettings = Seq(
  moduleName := projectName,
  organization := "com.lambdista",
  scalaVersion := "2.11.8",
//  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  (unmanagedSourceDirectories in Compile) <<= (scalaSource in Compile)(Seq(_)),
  (unmanagedSourceDirectories in Test) <<= (scalaSource in Test)(Seq(_)),
  scalacOptions := Seq(
    "-feature",
    "-language:dynamics",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-encoding",
    "utf8",
    "-deprecation",
    "-unchecked"
  ),
  scalacOptions ++= Seq("-Ypatmat-exhaust-depth", "off"),
  scalafmtConfig := Some(file(".scalafmt.conf")),
  initialCommands in console :=
    """
      |import scala.util._
      |import scala.concurrent.duration._
      |import scala.concurrent.duration.Duration._
      |import com.lambdista.config._
    """.stripMargin
)

lazy val config = (project in file("."))
  .aggregate(core, typesafe)
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    moduleName := "config-root",
    (unmanagedSourceDirectories in Compile) := Nil,
    (unmanagedSourceDirectories in Test) := Nil,
    publish := (),
    publishLocal := (),
    publishArtifact := false
  )

lazy val core = (project in file("core"))
  .settings(commonSettings ++ Publishing.settings: _*)
  .settings(
    moduleName := projectName,
    libraryDependencies ++= coreDeps,
    dependencyOverrides += shapeless
  )

lazy val typesafe = (project in file("typesafe"))
  .dependsOn(core)
  .settings(commonSettings ++ Publishing.settings: _*)
  .settings(
    moduleName := "config-typesafe",
    libraryDependencies ++= typesafeDeps
  )
