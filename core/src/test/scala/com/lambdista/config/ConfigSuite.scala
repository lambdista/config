package com.lambdista
package config

import java.io.InputStream
import java.nio.file.Paths
import java.util.UUID

import scala.concurrent.duration._

/**
  * Unit Test for Config.
  *
  * @author Alessandro Lacava
  * @since 2016-01-12
  */
class ConfigSuite extends UnitSuite {
  val confStr: String =
    """{
      |string = "hello",
      |duration = 5 day,
      |charRange = 'a' to 'z',
      |opt = null
      |}""".stripMargin
  val configFromStr: Result[Config] = Config.from(confStr)

  val confPath: String       = "core/src/test/resources/foo.conf"
  val config: Result[Config] = Config.from(Paths.get(confPath))

  def convertTo[A: ConcreteValue](key: String): Result[A] =
    for {
      c <- config
      a <- c.getAs[A](key)
    } yield a

  case class Greek(alpha: String, beta: Int)

  case class FooConfig(
    bar: String,
    baz: Option[Int],
    list: List[Int],
    mapList: List[Greek],
    range: Range,
    duration: Duration
  )

  case class SoftFoo(alpha: Int, baz: Int, bar: String)
  case class SoftMerge(foo: SoftFoo, baz: Int, zoo: String)

  case class HardFoo(baz: Int, bar: String)
  case class HardMerge(foo: HardFoo, baz: Int, zoo: String)

  def areSuccesses(tries: Result[_]*): Boolean = tries.forall(_.isRight)

  test("A well-formed config represented by a string should be loaded correctly") {
    assert(configFromStr.isRight)
  }

  test("it should encode its values correctly") {
    val string: Result[String] = for {
      c <- configFromStr
      v <- c.getAs[String]("string")
    } yield v

    val duration: Result[Duration] = for {
      c <- configFromStr
      v <- c.getAs[Duration]("duration")
    } yield v

    val charRange: Result[Vector[Char]] = for {
      c <- configFromStr
      v <- c.getAs[Vector[Char]]("charRange")
    } yield v

    val opt: Result[Option[Int]] = for {
      c <- configFromStr
      v <- c.getAs[Option[Int]]("opt")
    } yield v

    assert(areSuccesses(string, duration, charRange, opt))
    assert(string.contains("hello"))
    assert(duration.contains(5.days))
    assert(charRange.contains('a' to 'z'))
    assert(opt.contains(None))
  }

  test("A well-formed config represented by a resource in the class path should be loaded correctly") {
    val is: InputStream        = getClass.getResourceAsStream("/foo.conf")
    val config: Result[Config] = Config.from(is)

    assert(config.isRight)
  }

  test("A well-formed config represented by a Path should be loaded correctly") {
    assert(config.isRight)
  }

  test(
    "A config should be entirely convertible into a case class provided that its keys match the case class field names"
  ) {
    // Note how baz: Option[Int] is converted correctly. If, in the config file, baz were null, the Option value would've
    // been None. Furthermore, conversions from Range to List or Vector or Set and List to Vector or Set happens automatically
    val fooConfig: Result[FooConfig] = for {
      c <- config
      a <- c.as[FooConfig]
    } yield a

    assert(fooConfig.isRight)
  }

  test("A config whose keys do not match exactly the case class field names should be transformable so that they match") {
    val confPath: String       = "core/src/test/resources/fooish.conf"
    val config: Result[Config] = Config.from(Paths.get(confPath))

    val fooConfig: Result[FooConfig] = for {
      c <- config
      // first convert the Config into a AbstractMap...
      map <- c.as[AbstractMap]
      // ...then transform AbstractMap keys to match the case class field names
      newMap = map.transformKeys {
        case "rangefoo" => "range"
      }
      // Note how a given AbstractMap can be converted intto a case class too
      fooConf <- newMap.as[FooConfig]
    } yield fooConf

    assert(fooConfig.isRight)
  }

  test("A config should allow conversions into a case class also for any of its elements") {
    val greeks: Result[List[Greek]] = for {
      c <- config
      a <- c.getAs[List[Greek]]("mapList")
    } yield a

    assert(greeks.isRight)
  }

  test("A range element config element should be convertible into a List, but also into a Vector or Set") {
    val range: Result[Range]               = convertTo[Range]("range")
    val rangeAsList: Result[List[Int]]     = convertTo[List[Int]]("range")
    val rangeAsVector: Result[Vector[Int]] = convertTo[Vector[Int]]("range")
    val rangeAsSet: Result[Set[Int]]       = convertTo[Set[Int]]("range")

    assert(areSuccesses(range, rangeAsList, rangeAsVector, rangeAsSet))
  }

  test("A list config element should be convertible into a List, but also into a Vector or Set") {
    val list: Result[List[Greek]]           = convertTo[List[Greek]]("mapList")
    val listAsVector: Result[Vector[Greek]] = convertTo[Vector[Greek]]("mapList")
    val listAsSet: Result[Set[Greek]]       = convertTo[Set[Greek]]("mapList")

    assert(areSuccesses(list, listAsVector, listAsSet))
  }

  test("config elements should be traversable in a dynamic way") {
    val configStr: String =
      """
        |{
        |  authMode = "SCRAM-SHA-1",
        |  timeout = 5 seconds,
        |  connectionsPerNode = 10,
        |  keepAlive = true,
        |  failover = {
        |    initialDelay = 1 second,
        |    retries = 15,
        |    delayFactor = 1.0
        |  }
        |}
      """.stripMargin

    val config: Result[Config] = Config.from(configStr)

    val result: Result[Duration] = for {
      c            <- config
      initialDelay <- c.failover.initialDelay.as[Duration]
    } yield initialDelay

    assertEquals(result, Right(1.second))
  }

  test("Asking for a missing key dynamically should produce an error") {
    val configStr: String =
      """
        |{
        |  authMode = "SCRAM-SHA-1",
        |  timeout = 5 seconds,
        |  connectionsPerNode = 10,
        |  keepAlive = true,
        |  failover = {
        |    initialDelay = 1 second,
        |    retries = 15,
        |    delayFactor = 1.0
        |  }
        |}
      """.stripMargin

    val config: Result[Config] = Config.from(configStr)

    val result: Result[Int] = for {
      c          <- config
      missingKey <- c.failover.missingKey.as[Int]
    } yield missingKey

    assert {
      result match {
        case Left(_: KeyNotFoundError) => true
        case _                         => false
      }
    }
  }

  test("Trying to convert to the wrong type an existing config element, retrieved dynamically, should produce an error") {
    val configStr: String =
      """
        |{
        |  authMode = "SCRAM-SHA-1",
        |  timeout = 5 seconds,
        |  connectionsPerNode = 10,
        |  keepAlive = true,
        |  failover = {
        |    initialDelay = 1 second,
        |    retries = 15,
        |    delayFactor = 1.0
        |  }
        |}
      """.stripMargin

    val config: Result[Config] = Config.from(configStr)

    val result: Result[Int] = for {
      c            <- config
      initialDelay <- c.failover.initialDelay.as[Int]
    } yield initialDelay

    assert {
      result match {
        case Left(_: ConversionError) => true
        case _                        => false
      }
    }
  }

  test("A config should be convertible into a case class with other nested case classes") {
    val configStr: String =
      """
        |{
        |  authMode = "SCRAM-SHA-1",
        |  timeout = 5 seconds,
        |  connectionsPerNode = 10,
        |  keepAlive = true,
        |  failover = {
        |    initialDelay = 1 second,
        |    retries = 15,
        |    delayFactor = 1.0
        |  }
        |}
      """.stripMargin

    val config: Result[Config] = Config.from(configStr)

    case class Failover(initialDelay: Duration, retries: Int, delayFactor: Double)
    case class ConnOptions(
      authMode: String,
      timeout: Duration,
      connectionsPerNode: Int,
      keepAlive: Boolean,
      failover: Failover
    )

    val result: Result[ConnOptions] = config.flatMap(_.as[ConnOptions])

    assert(areSuccesses(result))
  }

  test(
    "A map config element should be convertible into a Map[String, A] too. Provided an instance of ConcreteValue[A] exists for A"
  ) {
    val confStr: String        = "{ map = { foo = 42, bar = 24} }"
    val config: Result[Config] = Config.from(confStr)

    val mapList: Result[Map[String, Int]] = for {
      c <- config
      m <- c.getAs[Map[String, Int]]("map")
    } yield m

    assert(mapList.isRight)
  }

  test(
    "The result of softly merging two configs should be a new config where, given a key, if the correspondent value is a map then the rhs config's value is " +
      "*softly* merged to the lhs config's value otherwise the rhs config's value replaces the lhs config's value."
  ) {
    val confStr1: String =
      """
        |{
        |  foo = {
        |    alpha = 1,
        |    bar = "hello"
        |  },
        |  baz = 42
        |}
      """.stripMargin
    val confStr2: String =
      """
        |{
        |  foo = {
        |    baz = 15,
        |    bar = "goodbye"
        |  },
        |  baz = 1,
        |  zoo = "hi"
        |}
      """.stripMargin

    val config1: Result[Config] = Config.from(confStr1)
    val config2: Result[Config] = Config.from(confStr2)

    val config: Result[Config] = for {
      c1 <- config1
      c2 <- config2
    } yield c1.recursivelyMerge(c2)

    val person: Result[SoftMerge] = for {
      c <- config
      p <- c.as[SoftMerge]
    } yield p
    val expected: Result[SoftMerge] =
      Right(SoftMerge(SoftFoo(alpha = 1, baz = 15, bar = "goodbye"), baz = 1, zoo = "hi"))
    assertEquals(person, expected)
  }

  test(
    "The result of hardly merging two configs be similar to Scala's default behaviour when using `++` between two `Map`s and the rhs config's values " +
      "replace entirely the lhs config's values with the same key"
  ) {
    val confStr1: String =
      """
        |{
        |  foo = {
        |    alpha = 1,
        |    bar = "hello"
        |  },
        |  zoo = "hi",
        |  baz = 42
        |}
      """.stripMargin
    val confStr2: String =
      """
        |{
        |  foo = {
        |    baz = 15,
        |    bar = "goodbye"
        |  },
        |  baz = 1
        |}
      """.stripMargin

    val config1: Result[Config] = Config.from(confStr1)
    val config2: Result[Config] = Config.from(confStr2)

    val config: Result[Config] = for {
      c1 <- config1
      c2 <- config2
    } yield c1.recursivelyMerge(c2)

    val person: Result[HardMerge] = for {
      c <- config
      p <- c.as[HardMerge]
    } yield p
    val expected: Result[HardMerge] = Right(HardMerge(HardFoo(baz = 15, bar = "goodbye"), baz = 1, zoo = "hi"))
    assertEquals(person, expected)
  }

  test("A config value should be callable using the \"dot\" syntax") {
    val confStr: String =
      """{
        |  foo = {bar = 42}
        |}
      """.stripMargin

    val config: Result[Config] = Config.from(confStr)

    val bar: Result[Int] = for {
      c   <- config
      bar <- c.getAs[Int]("foo.bar")
    } yield bar
    val expected: Result[Int] = Right(42)
    assertEquals(bar, expected)
  }

  test("A config string should work if it contains escaped chars") {
    val confStr: String =
      """{
        |  foo = "\"hello world\""
        |}
      """.stripMargin

    val config: Result[Config] = Config.from(confStr)

    val foo: Result[String] = for {
      c <- config
      f <- c.getAs[String]("foo")
    } yield f
    val expected: Result[String] = Right("\\\"hello world\\\"")
    assertEquals(foo, expected)
  }

  test("An ill-formed config should fail with a ConfigSyntaxError") {
    val confStr: String        = "{a = 42"
    val config: Result[Config] = Config.from(confStr)
    assert {
      config match {
        case Left(_: ConfigSyntaxError) => true
        case _                          => false
      }
    }
  }

  test("A config element not convertible to a given type should fail with a ConversionError") {
    val confStr: String        = "{int = 42}"
    val config: Result[Config] = Config.from(confStr)
    val string: Result[String] = config.flatMap(_.getAs[String]("int"))
    assert {
      string match {
        case Left(_: ConversionError) => true
        case _                        => false
      }
    }
  }

  test("A config element that does not exist should fail with a KeyNotFoundError") {
    val confStr: String        = "{int = 42}"
    val config: Result[Config] = Config.from(confStr)
    val int: Result[Int]       = config.flatMap(_.getAs[Int]("nonexistentKey"))
    assert {
      int match {
        case Left(_: KeyNotFoundError) => true
        case _                         => false
      }
    }
  }

  test("A config should support comments") {
    case class Conf(
      omg: String,
      bool: Boolean,
      betweenQuotes: Double,
      infinite: Duration,
      finite: Duration,
      charRange: List[Char],
      intRange: Range,
      array: List[Int]
    )

    val result: Result[Config] = ConfigParser.parse(""" // comment 1
          |{
          |// comment 2
          |omg   = "123",
          |bool = true,
          |"betweenQuotes": 12.4123, # comment 3
          |infinite = Inf,
          |finite = 5 millis,
          |# comment 4
          |intRange: 0 to 4 by 2,
          |charRange: 'a' to 'c',
          |array = [1, // comment 5
          |2, 3]
        |}""".stripMargin)

    val conf = result.flatMap(_.as[Conf])

    assert(result.isRight)
    assert(conf.isRight)
  }

  test("it support single-line comments repeated multiple times") {
    val result: Result[Config] = ConfigParser.parse("""
        |// comment 1
        |# comment 2
        |// comment 3
        |// comment 4
        |{
        |  bar = "hello",
        |  # comment 2
        |  baz = 42,
        |  list = [1, // comment 3
        |          2, 3],
        |  map = {
        |    alpha = "hello",
        |    beta = 42
        |  },
        |  mapList = [
        |    {
        |      alpha = "hello",
        |      beta = 42
        |    },
        |    {
        |      alpha = "world",
        |      beta = 24
        |    }
        |  ],
        |  range = 2 to 10 by 2,
        |  duration = 5 seconds
        |}
      """.stripMargin)

    val conf: Result[Map[String, AbstractValue]] = result.flatMap(_.as[Map[String, AbstractValue]])

    conf.foreach(println)

    assert(result.isRight)
    assert(conf.isRight)
  }

  test("A sealed trait should be valid to use in config") {
    sealed trait Foo
    final case class Bar(a: Int, b: Option[String]) extends Foo
    final case class Baz(z: Int)                    extends Foo

    val barCfg: String = """
        {
          a: 42,
          b: "hello"
        }
    """

    val bazCfg: String = """
        {
          z: 1
        }
    """

    val barFoo: Result[Foo] = for {
      cfg <- Config.from(barCfg)
      foo <- cfg.as[Foo]
    } yield foo

    val bazFoo: Result[Foo] = for {
      cfg <- Config.from(bazCfg)
      foo <- cfg.as[Foo]
    } yield foo

    assert(barFoo.contains(Bar(42, Some("hello"))))
    assert(bazFoo.contains(Baz(1)))
  }

  test("A custom ConcreteValue instances should work correctly") {
    final case class RawString(s: String)
    object RawString {
      def fromString(s: String): RawString = {
        RawString(s.replace("\\\"", "\""))
      }

      implicit val concreteValue: ConcreteValue[RawString] = new ConcreteValue[RawString] {
        override def apply(abstractValue: AbstractValue): Option[RawString] = {
          abstractValue match {
            case AbstractString(value) => Some(RawString.fromString(value))
            case _                     => None
          }
        }
      }
    }

    val confStr: String        = """
      {
        bar = "a\"b"
      }
    """
    val config: Result[Config] = Config.from(confStr)
    final case class FooConfig(bar: RawString)

    val fooConfig: Result[FooConfig] = for {
      conf   <- config
      result <- conf.as[FooConfig]
    } yield result

    assert(fooConfig.map(_.bar).contains(RawString.fromString("a\"b")))
  }

  test("it should work correctly for UUID too") {
    val confStr: String = """
      {
        uuid = "238dfdf4-850d-4643-b4f3-019252515ed8"
      }
    """
    final case class Foo(uuid: UUID)
    implicit val uuidCv: ConcreteValue[UUID] = new ConcreteValue[UUID] {
      override def apply(abstractValue: AbstractValue): Option[UUID] = abstractValue match {
        case AbstractString(x) => Result.attempt(UUID.fromString(x)).toOption
        case _                 => None
      }
    }
    val foo: Result[Foo] = for {
      conf   <- Config.from(confStr)
      result <- conf.as[Foo]
    } yield result
    println(s"foo: $foo")
    assert(foo.map(_.uuid).contains(UUID.fromString("238dfdf4-850d-4643-b4f3-019252515ed8")))
  }

  test("Missing values in config should be converted into case classes where those values are of type Option[A]") {
    case class Foo(a: Int, b: Option[String], c: String, d: Option[Double])
    case class Bar(foo: Foo, x: Option[Int], y: List[String], z: Option[List[Int]])

    val cfgStr =
      """{
        | foo: {
        |   a: 42,
        |   c: "hello",
        |   d: 1
        |   },
        | y: ["hello", "world"]
        |}
      """.stripMargin

    val result = Config.from(cfgStr).flatMap(_.as[Bar])

    assert(result.isRight)
  }
}
