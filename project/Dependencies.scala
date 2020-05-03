import sbt._

object Dependencies {
  lazy val projectScalaVersion = "2.13.2"

  // Versions
  lazy val scalaTestVersion      = "3.1.1"
  lazy val typesafeConfigVersion = "1.4.0"
  lazy val fastparseVersion      = "2.1.3"
  lazy val fastparseVersion2_11  = "2.1.2"
  lazy val magnoliaVersion       = "0.16.0"

  // Libraries
  lazy val scalatest      = "org.scalatest"  %% "scalatest"    % scalaTestVersion % "test"
  lazy val typesafeConfig = "com.typesafe"   % "config"        % typesafeConfigVersion
  lazy val fastparse      = "com.lihaoyi"    %% "fastparse"    % fastparseVersion
  lazy val fastparse2_11  = "com.lihaoyi"    %% "fastparse"    % fastparseVersion2_11
  lazy val magnolia       = "com.propensive" %% "magnolia"     % magnoliaVersion
  lazy val scalaReflect   = "org.scala-lang" % "scala-reflect" % projectScalaVersion

  // Projects
  lazy val coreCommonDeps = Seq(
    scalatest,
    scalaReflect,
    magnolia
  )
  lazy val coreDeps     = coreCommonDeps :+ fastparse
  lazy val coreDeps2_11 = coreCommonDeps :+ fastparse2_11

  lazy val typesafeDeps = Seq(typesafeConfig, scalatest)
}
