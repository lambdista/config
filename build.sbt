import Dependencies._

lazy val projectName = "config"

lazy val commonSettings = Seq(
  moduleName := projectName,
  version := "0.3.0",
  organization := "com.lambdista",
  scalaVersion := "2.11.8",
  (unmanagedSourceDirectories in Compile) <<= (scalaSource in Compile) (Seq(_)),
  (unmanagedSourceDirectories in Test) <<= (scalaSource in Test) (Seq(_)),
  scalacOptions := Seq(
    "-feature",
    "-language:dynamics",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-encoding", "utf8",
    "-deprecation",
    "-unchecked"),
  scalacOptions ++= Seq("-Ypatmat-exhaust-depth", "off"),
  initialCommands in console :=
    """
      |import com.lambdista.config._
    """.stripMargin
)

lazy val config = (project in file("."))
  .aggregate(core)
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

