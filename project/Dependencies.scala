import sbt._

object Dependencies {
  lazy val projectScalaVersion = "2.13.2"

  // Versions
  lazy val munitVersion          = "0.7.8"
  lazy val typesafeConfigVersion = "1.4.0"
  lazy val fastparseVersion      = "2.3.0"
  lazy val magnoliaVersion       = "0.16.0"

  // Libraries
  lazy val munit          = "org.scalameta"  %% "munit"        % munitVersion % Test
  lazy val typesafeConfig = "com.typesafe"   % "config"        % typesafeConfigVersion
  lazy val fastparse      = "com.lihaoyi"    %% "fastparse"    % fastparseVersion
  lazy val magnolia       = "com.propensive" %% "magnolia"     % magnoliaVersion
  lazy val scalaReflect   = "org.scala-lang" % "scala-reflect" % projectScalaVersion

  // Projects
  lazy val coreCommonDeps = Seq(
    munit,
    scalaReflect,
    magnolia
  )
  lazy val coreDeps = coreCommonDeps :+ fastparse

  lazy val typesafeDeps = Seq(typesafeConfig, munit)
}
