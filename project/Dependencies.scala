import sbt._

object Dependencies {
  // Versions
  lazy val scalaTestVersion      = "3.0.0"
  lazy val shapelessVersion      = "2.3.2"
  lazy val parboiledVersion      = "2.1.3"
  lazy val typesafeConfigVersion = "1.3.1"
  lazy val fastparseVersion      = "0.4.2"

  // Libraries
  val compPlugin = compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  val scalatest      = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val shapeless      = "com.chuusai"   %% "shapeless" % shapelessVersion
  val typesafeConfig = "com.typesafe"  % "config"     % typesafeConfigVersion
  val fastparse      = "com.lihaoyi"   %% "fastparse" % fastparseVersion

  // Projects
  val coreDeps = Seq(
    scalatest,
    shapeless,
    fastparse,
    compPlugin
  )

  val typesafeDeps = Seq(typesafeConfig, scalatest, compPlugin)
}
