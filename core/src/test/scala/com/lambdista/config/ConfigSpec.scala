package com.lambdista
package config

import java.io.InputStream
import java.nio.file.Paths

import scala.concurrent.duration._
import scala.util.{Success, Try}

/**
  * Unit Test for Config.
  *
  * @author Alessandro Lacava 
  * @since 2016-01-12
  */
class ConfigSpec extends UnitSpec {
  val confStr =
    """{
      |string = "hello",
      |duration = 5 day,
      |charRange = 'a' to 'z',
      |opt = null
      |}""".stripMargin
  val configFromStr: Try[Config] = Config.from(confStr)

  val confPath = "core/src/test/resources/foo.conf"
  val config: Try[Config] = Config.from(Paths.get(confPath))

  def convertTo[A: ConcreteValue](key: String) = for {
    c <- config
    a <- c.tryGet[A](key)
  } yield a

  case class Greek(alpha: String, beta: Int)

  case class FooConfig(bar: String, baz: Option[Int], list: List[Int], mapList: List[Greek], range: Range, duration: Duration)

  case class Person(name: String, age: Int)

  def areSuccesses(tries: Try[_]*): Boolean = tries.forall(_.isSuccess)

  "A well-formed config represented by a string" should "be loaded correctly" in {
    assert(configFromStr.isSuccess)
  }

  it should "encode its values correctly" in {
    val string: Try[String] = for {
      c <- configFromStr
      v <- c.tryGet[String]("string")
    } yield v

    val duration: Try[Duration] = for {
      c <- configFromStr
      v <- c.tryGet[Duration]("duration")
    } yield v

    val charRange: Try[Vector[Char]] = for {
      c <- configFromStr
      v <- c.tryGet[Vector[Char]]("charRange")
    } yield v

    val opt: Try[Option[Int]] = for {
      c <- configFromStr
      v <- c.tryGet[Option[Int]]("opt")
    } yield v

    assert(areSuccesses(string, duration, charRange, opt))

    string.get shouldBe "hello"

    duration.get shouldBe (5 days)

    charRange.get shouldBe ('a' to 'z')

    opt.get shouldBe None
  }

  "A well-formed config represented by a resource in the class path" should "be loaded correctly" in {
    val is: InputStream = getClass.getResourceAsStream("/foo.conf")
    val config: Try[Config] = Config.from(is)

    assert(config.isSuccess)
  }

  "A well-formed config represented by a Path" should "be loaded correctly" in {
    assert(config.isSuccess)
  }

  "A config" should "be entirely convertible into a case class provided that its keys match the case class field names" in {
    // Note how baz: Option[Int] is converted correctly. If, in the config file, baz were null, the Option value would've
    // been None. Furthermore, conversions from Range to List or Vector or Set and List to Vector or Set happens automatically
    val fooConfig: Try[FooConfig] = for {
      c <- config
      a <- c.tryAs[FooConfig]
    } yield a

    assert(fooConfig.isSuccess)
  }

  "A config whose keys do not match exactly the case class field names" should "be transformable so that they match" in {
    val confPath = "core/src/test/resources/fooish.conf"
    val config: Try[Config] = Config.from(Paths.get(confPath))

    val fooConfig: Try[FooConfig] = for {
      c <- config
      // first convert the Config into a ConfigMap...
      map <- c.tryAs[AbstractMap]
      // ...then transform ConfigMap keys to match the case class field names
      newMap = map.transformKeys {
        case "range foo" => "range"
      }
      // Note how a given ConfigMap can be converted intto a case class too
      fooConf <- newMap.tryAs[FooConfig]
    } yield fooConf

    assert(fooConfig.isSuccess)
  }

  "A config" should "allow conversions into a case class also for any of its elements" in {
    val greeks: Try[List[Greek]] = for {
      c <- config
      a <- c.tryGet[List[Greek]]("mapList")
    } yield a

    assert(greeks.isSuccess)
  }

  "A range element config element" should "be convertible into a List, but also into a Vector or Set" in {
    val range: Try[Range] = convertTo[Range]("range")
    val rangeAsList: Try[List[Int]] = convertTo[List[Int]]("range")
    val rangeAsVector: Try[Vector[Int]] = convertTo[Vector[Int]]("range")
    val rangeAsSet: Try[Set[Int]] = convertTo[Set[Int]]("range")

    assert(areSuccesses(range, rangeAsList, rangeAsVector, rangeAsSet))
  }

  "A list config element" should "be convertible into a List, but also into a Vector or Set" in {
    val list: Try[List[Greek]] = convertTo[List[Greek]]("mapList")
    val listAsVector: Try[Vector[Greek]] = convertTo[Vector[Greek]]("mapList")
    val listAsSet: Try[Set[Greek]] = convertTo[Set[Greek]]("mapList")

    assert(areSuccesses(list, listAsVector, listAsSet))
  }

  "A map config element" should "be convertible into a Map[String, A] too. Provided an instance of ConcreteValue[A] exists for A" in {
    val confStr = "{ map = { foo = 42, bar = 24} }"
    val config: Try[Config] = Config.from(confStr)

    val mapList: Try[Map[String, Int]] = for {
      c <- config
      m <- c.tryGet[Map[String, Int]]("map")
    } yield m

    assert(mapList.isSuccess)
  }

  "The result of merging two configs" should
    "be a new config whose elements are those of the lhs config (with respect to the merge method) plus those of the rhs config" in {
    val confStr1 = """{name: "John"}"""
    val confStr2 = """{age: 42}"""

    val config1: Try[Config] = Config.from(confStr1)
    val config2: Try[Config] = Config.from(confStr2)

    val config: Try[Config] = for {
      c1 <- config1
      c2 <- config2
    } yield c1.merge(c2)

    val person: Try[Person] = for {
      c <- config
      p <- c.tryAs[Person]
    } yield p

    person should matchPattern { case Success(Person("John", 42)) => }
  }

  it should "replace the lhs config elements with the rhs config elements in case of key collisions" in {
    val confStr1 = """{name: "John"}"""
    val confStr2 = """{name: "Jane", age: 42}"""

    val config1: Try[Config] = Config.from(confStr1)
    val config2: Try[Config] = Config.from(confStr2)

    val config: Try[Config] = for {
      c1 <- config1
      c2 <- config2
    } yield c1.merge(c2)

    val person: Try[Person] = for {
      c <- config
      p <- c.tryAs[Person]
    } yield p

    // Jane replace John as the value of the key `name`
    person should matchPattern { case Success(Person("Jane", 42)) => }
  }

  "A config value" should "be callable using the \"dot\" syntax" in {
    val confStr =
      """{
        |foo = {bar = 42}
        |}
      """.stripMargin

    val config: Try[Config] = Config.from(confStr)

    val bar: Try[Int] = for {
      c <- config
      bar <- c.tryGet[Int]("foo.bar")
    } yield bar

    bar should matchPattern { case Success(42) => }
  }

  "An ill-formed config" should "fail with a ConfigSyntaxException" in {
    val confStr = "{a = 42"

    val config: Try[Config] = Config.from(confStr)

    assert(config.isFailure)

    intercept[ConfigSyntaxException] {
      config.get
    }
  }

  "A config element not convertible to a given type" should "fail with a ConversionException" in {
    val confStr = "{int = 42}"

    val config: Try[Config] = Config.from(confStr)

    val string: Try[String] = config.flatMap(_.tryGet[String]("int"))

    assert(string.isFailure)

    intercept[ConversionException] {
      string.get
    }
  }

  "A config element that does not exist" should "fail with a KeyNotFoundException" in {
    val confStr = "{int = 42}"

    val config: Try[Config] = Config.from(confStr)

    val int: Try[Int] = config.flatMap(_.tryGet[Int]("nonexistentKey"))

    assert(int.isFailure)

    intercept[KeyNotFoundException] {
      int.get
    }
  }
}