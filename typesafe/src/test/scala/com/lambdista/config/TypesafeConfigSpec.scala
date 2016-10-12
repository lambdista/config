package com.lambdista
package config

import java.io.File

import scala.util.Try

import com.typesafe.config.{ConfigFactory, Config => TSConfig}

// important to bring into scope the ConfigLoader instance for Typesafe's Config
import com.lambdista.config.typesafe._

/**
  *
  *
  * @author Alessandro Lacava
  * @since 2016-01-12
  */
class TypesafeConfigSpec extends UnitSpec {
  case class Person(firstName: String, lastName: String)

  case class TypesafeConfig(
      string: String,
      int: Int,
      double: Double,
      boolean: Boolean,
      list: List[Int],
      mapList: List[Person]
  )

  "typesafe.conf using HOCON syntax used by the Typesafe config library" should "be loaded and converted correctly" in {
    val confPath           = "typesafe/src/test/resources/typesafe.conf"
    val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

    val config: Try[Config] = Config.from(tsConfig)

    val typesafeConfig: Try[TypesafeConfig] = config.flatMap(_.as[TypesafeConfig])

    assert(typesafeConfig.isSuccess)
  }

  case class Ws(url: String, key: String)

  case class Db(name: String, host: String, port: Int, user: String, password: String)

  case class Configuration(version: String, wss: List[Ws], home: String, db: Option[Db])

  "typesafe2.conf using HOCON syntax used by the Typesafe config library" should "be loaded and converted correctly" in {
    val confPath           = "typesafe/src/test/resources/typesafe2.conf"
    val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

    val configuration: Try[Configuration] = for {
      conf <- Config.from(tsConfig)
      res  <- conf.as[Configuration]
    } yield res

    assert(configuration.isSuccess)
  }
}
