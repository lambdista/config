package com.lambdista
package config

import java.io.File

import com.typesafe.config.{ConfigFactory, Config => TSConfig}

// important to bring into scope the ConfigLoader instance for Typesafe's Config
import com.lambdista.config.typesafe._

/**
  *
  *
  * @author Alessandro Lacava
  * @since 2016-01-12
  */
class TypesafeConfigSuite extends UnitSuite {
  case class Person(firstName: String, lastName: String)

  case class TypesafeConfig(
    string: String,
    int: Int,
    double: Double,
    boolean: Boolean,
    list: List[Int],
    mapList: List[Person]
  )

  test("typesafe.conf using HOCON syntax used by the Typesafe config library should be loaded and converted correctly") {
    val confPath           = "typesafe/src/test/resources/typesafe.conf"
    val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

    val config: Result[Config] = Config.from(tsConfig)

    val typesafeConfig: Result[TypesafeConfig] = config.flatMap(_.as[TypesafeConfig])

    assert(typesafeConfig.isRight)
  }

  case class Ws(url: String, key: String)

  case class Db(name: String, host: String, port: Int, user: String, password: String)

  case class Configuration(version: String, wss: List[Ws], home: String, db: Option[Db])

  test("typesafe2.conf using HOCON syntax used by the Typesafe config library should be loaded and converted correctly") {
    val confPath           = "typesafe/src/test/resources/typesafe2.conf"
    val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

    val configuration: Result[Configuration] = for {
      conf <- Config.from(tsConfig)
      res  <- conf.as[Configuration]
    } yield res
    assert(configuration.isRight)
  }

  test("Missing values in config should be converted into case classes where those values are of type Option[A]") {
    case class Foo(a: Int, b: Option[String], c: String, d: Option[Double])
    case class Bar(foo: Foo, x: Option[Int], y: List[String], z: Option[List[Int]])

    val cfgStr =
      """foo: {
        |   a: 42
        |   c: "hello"
        |   d: 1
        |   }
        | y: ["hello", "world"]
        | z: [1, 2, 3]
      """.stripMargin

    val tsConfig = ConfigFactory.parseString(cfgStr)
    val result   = Config.from(tsConfig).flatMap(_.as[Bar])
    assert(result.isRight)
  }
}
