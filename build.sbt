import Dependencies._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import com.typesafe.sbt.SbtSite.SiteKeys._

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

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val config = (project in file("."))
  .aggregate(core, typesafe)
  .dependsOn(core, typesafe)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := "config-root",
    (unmanagedSourceDirectories in Compile) := Nil,
    (unmanagedSourceDirectories in Test) := Nil
  )

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(Publishing.settings)
  .settings(
    moduleName := projectName,
    libraryDependencies ++= coreDeps,
    dependencyOverrides += shapeless
  )

lazy val typesafe = (project in file("typesafe"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(Publishing.settings)
  .settings(
    moduleName := "config-typesafe",
    libraryDependencies ++= typesafeDeps
  )

lazy val docSettings = tutSettings ++ site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
    ghpagesNoJekyll := false,
    git.remoteRepo := "https://github.com/lambdista/config",
    includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
  )

lazy val docs = project
  .dependsOn(core, typesafe)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(docSettings)
  .settings(
    moduleName := "config-docs",
    tutSourceDirectory := file("docs/src/tut"),
    tutTargetDirectory := file(".")
  )
