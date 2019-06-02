import sbt._

object Dependencies {
  // Versions
  lazy val scalaTestVersion      = "3.1.0-SNAP10"
  lazy val shapelessVersion      = "2.3.3"
  lazy val typesafeConfigVersion = "1.3.4"
  lazy val fastparseVersion      = "2.1.2"

  // Libraries
  val scalatest      = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val shapeless      = "com.chuusai"   %% "shapeless" % shapelessVersion
  val typesafeConfig = "com.typesafe"  % "config"     % typesafeConfigVersion
  val fastparse      = "com.lihaoyi"   %% "fastparse" % fastparseVersion

  // Projects
  val coreDeps = Seq(
    scalatest,
    shapeless,
    fastparse
  )

  val typesafeDeps = Seq(typesafeConfig, scalatest)
}
