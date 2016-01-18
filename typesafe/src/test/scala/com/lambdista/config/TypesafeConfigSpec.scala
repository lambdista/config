package com.lambdista
package config

import java.io.File

import scala.util.Try

import com.typesafe.config.{Config => TSConfig, ConfigFactory}

import com.lambdista.config.typesafe._ // important to bring into scope the ConfigLoader instance for Typesafe's Config

/**
  *
  *
  * @author Alessandro Lacava 
  * @since 2016-01-12
  */
class TypesafeConfigSpec extends UnitSpec {

  case class Person(firstName: String, lastName: String)

  case class TypesafeConfig(string: String, int: Int, double: Double, boolean: Boolean, list: List[Int], mapList: List[Person])

  "A config using HOCON syntax used by the Typesafe config library" should "be loaded and converted correctly" in {
    val confPath = "typesafe/src/test/resources/typesafe.conf"
    val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

    val config: Try[Config] = Config.from(tsConfig)

    val typesafeConfig: Try[TypesafeConfig] = config.flatMap(_.tryAs[TypesafeConfig])

    assert(typesafeConfig.isSuccess)
  }
}
