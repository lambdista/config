import sbt.Keys._
import sbt._

object ConfigBuild extends Build {
  lazy val projectName = "config"
  lazy val projectScalaVersion = "2.11.7"

  lazy val scalaTestVersion = "2.2.6"
  lazy val shapelessVersion = "2.2.5"
  lazy val parboiledVersion = "2.1.0"
  lazy val typesafeConfigVersion = "1.2.1"

  def commonSettings = Seq(
    moduleName := projectName,
    version := "0.2.1",
    organization := "com.lambdista",
    scalaVersion := projectScalaVersion,
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
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    ),
    initialCommands in console :=
      """
        |import com.lambdista.config._
      """.stripMargin
  )

  lazy val config = (project in file(".")
    aggregate core
    dependsOn core
    settings (commonSettings: _*)
    settings(
    moduleName := "config-root",
    (unmanagedSourceDirectories in Compile) := Nil,
    (unmanagedSourceDirectories in Test) := Nil,
    publish := (),
    publishLocal := (),
    publishArtifact := false
    )
    )

  lazy val core = (project
    settings (commonSettings ++ Publishing.settings: _*)
    settings(
    moduleName := projectName,
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % shapelessVersion,
      "org.parboiled" %% "parboiled" % parboiledVersion
    ),
    dependencyOverrides += "com.chuusai" %% "shapeless" % shapelessVersion
    )
    )

  lazy val typesafe = (project in file("typesafe")
    dependsOn core
    settings (commonSettings ++ Publishing.settings: _*)
    settings(
    moduleName := "config-typesafe",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % typesafeConfigVersion
    )
    )
    )
}
