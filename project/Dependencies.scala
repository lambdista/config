import sbt._

object Dependencies {
  lazy val projectScalaVersion = "2.13.0"

  // Versions
  lazy val scalaTestVersion      = "3.0.8"
  lazy val shapelessVersion      = "2.3.3"
  lazy val typesafeConfigVersion = "1.3.4"
  lazy val fastparseVersion      = "2.1.3"
  lazy val fastparseVersion2_11  = "2.1.2"

  // Libraries
  val scalatest      = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val shapeless      = "com.chuusai"   %% "shapeless" % shapelessVersion
  val typesafeConfig = "com.typesafe"  % "config"     % typesafeConfigVersion
  val fastparse      = "com.lihaoyi"   %% "fastparse" % fastparseVersion
  val fastparse2_11  = "com.lihaoyi"   %% "fastparse" % fastparseVersion2_11

  // Projects
  lazy val coreCommonDeps = Seq(
    scalatest,
    shapeless
  )
  lazy val coreDeps     = coreCommonDeps :+ fastparse
  lazy val coreDeps2_11 = coreCommonDeps :+ fastparse2_11

  lazy val typesafeDeps = Seq(typesafeConfig, scalatest)
}
