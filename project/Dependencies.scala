import sbt._

object Dependencies {
  lazy val projectScalaVersion = "2.13.6"

  // Versions
  lazy val munitVersion          = "0.7.27"
  lazy val typesafeConfigVersion = "1.4.1"
  lazy val fastparseVersion      = "2.3.2"
  lazy val magnoliaVersion       = "1.0.0-M5"

  // Libraries
  lazy val munit          = "org.scalameta"                %% "munit"         % munitVersion % Test
  lazy val typesafeConfig = "com.typesafe"                  % "config"        % typesafeConfigVersion
  lazy val fastparse      = "com.lihaoyi"                  %% "fastparse"     % fastparseVersion
  lazy val magnolia       = "com.softwaremill.magnolia1_2" %% "magnolia"      % magnoliaVersion
  lazy val scalaReflect   = "org.scala-lang"                % "scala-reflect" % projectScalaVersion

  // Projects
  lazy val coreCommonDeps = Seq(
    munit,
    scalaReflect,
    magnolia
  )
  lazy val coreDeps = coreCommonDeps :+ fastparse

  lazy val typesafeDeps = Seq(typesafeConfig, munit)
}
