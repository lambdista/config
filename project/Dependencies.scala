import sbt._

object Dependencies {
  // Versions
  lazy val scalaTestVersion      = "3.0.0"
  lazy val shapelessVersion      = "2.3.2"
  lazy val parboiledVersion      = "2.1.3"
  lazy val typesafeConfigVersion = "1.3.1"

  // Libraries
  val scalatest      = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val shapeless      = "com.chuusai"   %% "shapeless" % shapelessVersion
  val parboiled      = "org.parboiled" %% "parboiled" % parboiledVersion
  val typesafeConfig = "com.typesafe"  % "config"     % typesafeConfigVersion

  // Projects
  val coreDeps = Seq(
    scalatest,
    shapeless,
    parboiled,
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

  val typesafeDeps = Seq(typesafeConfig, scalatest)
}
