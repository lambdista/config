import sbt._

object Dependencies {
  // Versions
  lazy val scalaTestVersion = "2.2.6"
  lazy val shapelessVersion = "2.2.5"
  lazy val parboiledVersion = "2.1.0"
  lazy val typesafeConfigVersion = "1.2.1"

  // Libraries
  val scalatest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val shapeless = "com.chuusai" %% "shapeless" % shapelessVersion
  val parboiled = "org.parboiled" %% "parboiled" % parboiledVersion
  val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion

  // Projects
  val coreDeps = Seq(scalatest, shapeless, parboiled)

  val typesafeDeps = Seq(typesafeConfig, scalatest)
}
