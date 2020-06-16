[![Build Status](https://travis-ci.org/lambdista/config.svg?branch=master)](https://travis-ci.org/lambdista/config)
[![codecov.io](http://codecov.io/github/lambdista/config/coverage.svg?branch=master)](http://codecov.io/github/lambdista/config?branch=master)
[![Download](https://api.bintray.com/packages/lambdista/maven/config/images/download.svg)](https://bintray.com/lambdista/maven/config/_latestVersion)
[![Javadocs](https://javadoc.io/badge/com.lambdista/config_2.13.svg)](https://javadoc.io/doc/com.lambdista/config_2.13)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.lambdista/config_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.lambdista/config_2.13)
[![Latest version](https://index.scala-lang.org/lambdista/config/config/latest.svg?color=green&v=1)](https://index.scala-lang.org/lambdista/config/config)

# config: a type safe, purely functional configuration library for Scala

## Table of Contents
* [Not only another Typesafe's config wrapper](#notOnlyTypesafeConfig)
* [Configuration Syntax](#configSyntax)
* [Usage](#usage)
    * [Automatic conversion to case class](#caseClassConversion)
    * [Automatic conversion to sealed trait](#sealedTraitConversion)
    * [Automatic conversion to Map[String, A]](#mapConversion)
    * [Value-by-value conversion](#valueByValueConversion)
    * [Dynamic value-by-value conversion](#dynamicValueByValueConversion)
    * [Custom concrete value decoders](#customDecoders)
* [Config loaders](#configLoaders)
    * [Loading config from a simple String](#stringLoader)
    * [Loading a config from Typesafe Config](#typesafeLoader)
* [Merging two configurations](#mergingConfigs)
* [Scaladoc API](#scaladoc)
* [Bugs and Feedback](#feedback)
* [License](#license)

<a name="notOnlyTypesafeConfig"></a>
## Not only another Typesafe's config wrapper
Right from the start I didn't want to depend on other config libraries when I started implementing this one so I wrote
my own parser for a simple *JSONish* syntax. One of the advantages in using your own parser is you can add other custom
types. For example this lib allows you to define a 
Scala [Range](http://www.scala-lang.org/api/current/index.html#scala.collection.immutable.Range) while 
[HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) doesn't.
Hence, this is not only another [Typesafe's config](https://github.com/typesafehub/config) wrapper. However,
if you are already using Typesafe's config library and/or just prefer HOCON syntax for your configuration,
there's an adapter that will convert a Typesafe `Config` object into this config's AST.
See [this example](#typesafeLoader).

<a name="configSyntax"></a>
## Configuration Syntax
The syntax expected by this library is a JSON-superset. This means that any JSON file
should be a valid configuration. However, the `null` JSON values can only be converted to `Option[A]`, where `A` 
is the type you expect because, of course, we don't fancy `null` in Scala code. The *superset* part means that:
  
* You can optionally use `=` instead of `:`
* You can use comments: you can start a comment line using both `//` and `#`
* You can avoid putting the keys between quotes, unless your key contains white spaces
* You can use a Scala [Duration](http://www.scala-lang.org/api/current/index.html#scala.concurrent.duration.Duration)
* You can use a Scala [Range](http://www.scala-lang.org/api/current/index.html#scala.collection.immutable.Range)

<a name="usage"></a>
## Usage
As a first step you need to add the resolver and dependency to your build file:

```scala
libraryDependencies += "com.lambdista" %% "config" % "0.7.1"
```

Scala 2.13.x and 2.12.x are supported.

Ok, let's see the typical usage scenarios. As a use case consider the following configuration, unless otherwise specified:

```
// comment 1
{
  bar = "hello",
  # comment 2
  baz = 42,
  list = [1, // comment 3
          2, 3],
  map = {
    alpha = "hello",
    beta = 42
  },
  mapList = [
    {
      alpha = "hello",
      beta = 42
    },
    {
      alpha = "world",
      beta = 24
    }
  ],
  range = 2 to 10 by 2,
  duration = 5 seconds
}
```

Suppose the previous configuration is at the relative path: `core/src/test/resources/foo.conf`.

First thing first, load and parse your config:

```scala
import scala.concurrent.duration.Duration
import java.nio.file.Paths
import com.lambdista.config._

val confPath = "core/src/test/resources/foo.conf"
// confPath: String = "core/src/test/resources/foo.conf"
val config: Result[Config] = Config.from(Paths.get(confPath))
// config: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       HashMap(
//         "duration" -> AbstractDuration(5 seconds),
//         "range" -> AbstractRange(Range(2, 4, 6, 8, 10)),
//         "bar" -> AbstractString("hello"),
//         "mapList" -> AbstractList(
//           List(
//             AbstractMap(
//               Map(
//                 "alpha" -> AbstractString("hello"),
//                 "beta" -> AbstractNumber(42.0)
//               )
//             ),
//             AbstractMap(
//               Map(
//                 "alpha" -> AbstractString("world"),
//                 "beta" -> AbstractNumber(24.0)
//               )
//             )
//           )
//         ),
//         "baz" -> AbstractNumber(42.0),
//         "list" -> AbstractList(
//           List(AbstractNumber(1.0), AbstractNumber(2.0), AbstractNumber(3.0))
//         )
//       )
//     )
//   )
// )
```

Apart from `java.nio.file.Path` you can load your config from other resources using [Config Loaders](#configLoaders).
 
As you can see the result is a `Result[Config]` (where `Result[A]` is just a type alias for `Either[Error, A]`).
Indeed you can get two types of error here:

* The resource cannot be found.
* The resource can be found but its parsing failed.

In both cases you would get a `Left` wrapping the appropriate `Error` (a subclass of `Exception`).

Once you have a `Config` object you can do two main things with it:

* Convert it entirely into a case class representing the whole configuration.
* Retrieve a single value and convert it to whatever it's convertible to.

<a name="caseClassConversion"></a>
### Automatic conversion to case class
Here's how you would map the previous configuration to a case class (`config` is the value from the previous example):

```scala
case class Greek(alpha: String, beta: Int)

case class FooConfig(
    bar: String, 
    baz: Option[Int], 
    list: List[Int],
    map: Greek,
    mapList: List[Greek], 
    range: Range, 
    duration: Duration,
    missingValue: Option[String]
)

val fooConfig: Result[FooConfig] = for {
  conf <- config
  result <- conf.as[FooConfig]
} yield result
// fooConfig: Result[FooConfig] = Left(
//   com.lambdista.config.ConversionError: Could not convert {duration = 5 seconds, range = [2.0, 4.0, 6.0, 8.0, 10.0], bar = "hello", mapList = [{alpha = "hello", beta = 42.0}, {alpha = "world", beta = 24.0}], baz = 42.0, list = [1.0, 2.0, 3.0]} to the type requested
// )
```

The value of `fooConfig` will be:

```scala
Right(FooConfig(hello,Some(42),List(1, 2, 3),Greek(hello,42),List(Greek(hello,42), Greek(world,24)),Range(2, 4, 6, 8, 10),5 seconds))
```

Here you can already notice some interesting features of this library:

* The conversion to a case class happens automatically, no boilerplate on the client side is required.
* Since `baz` is declared as `Option[Int]` the library automatically wraps the `Int` value into a `Some`.
* By the way, note also how `missingValue` is not present in the config but since it's declared as `Option` in the
case class its value becomes `None`.
* The automatic conversion works also for nested structures, see `mapList` for example.
* `Range` and `Duration` work like a charm. Note that for both `Range` and `Duration` you can use the syntax you
would use in regular Scala code. For example, you could have used `5 secs` instead of `5 seconds` in `foo.conf` and
it would have worked smoothly.

<a name="sealedTraitConversion"></a>
### Automatic conversion to sealed trait
Example:
```scala
sealed trait Foo
final case class Bar(a: Int, b: Option[String]) extends Foo
final case class Baz(z: Int)                    extends Foo

val barCfg: String = """
    {
      a: 42,
      b: "hello"
    }
"""
// barCfg: String = """
//     {
//       a: 42,
//       b: "hello"
//     }
// """

val bazCfg: String = """
    {
      z: 1
    }
"""
// bazCfg: String = """
//     {
//       z: 1
//     }
// """

val barFoo: Result[Foo] = for {
  cfg <- Config.from(barCfg)
  foo <- cfg.as[Foo]
} yield foo
// barFoo: Result[Foo] = Right(Bar(42, Some("hello")))

val bazFoo: Result[Foo] = for {
  cfg <- Config.from(bazCfg)
  foo <- cfg.as[Foo]
} yield foo
// bazFoo: Result[Foo] = Right(Baz(1))
```
The value of `barFoo` will be:

```scala
Right(Bar(42,Some(hello)))
```
The value of `bazFoo` will be:

```scala
Right(Baz(1))
```

<a name="mapConversion"></a>
### Automatic conversion to `Map[String, A]`
You can convert a configuration to a `Map[String, A]`, provided that all the values in the `Map` have the same type,
`A`, and there exists an implicit instance of `ConcreteValue[A]` in scope. Most of the times this instance is
automatically generated as in the case class example so you don't need to worry about that.
Here's an example:

```scala
val cfgStr = """
{
  foo = 0,
  bar = 1,
  baz = 42
}
"""
// cfgStr: String = """
// {
//   foo = 0,
//   bar = 1,
//   baz = 42
// }
// """

val config: Result[Config] = Config.from(cfgStr)
// config: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractNumber(0.0),
//         "bar" -> AbstractNumber(1.0),
//         "baz" -> AbstractNumber(42.0)
//       )
//     )
//   )
// )

val confAsMap: Result[Map[String, Int]] = for {
  conf <- config
  result <- conf.as[Map[String, Int]]
} yield result
// confAsMap: Result[Map[String, Int]] = Right(
//   Map("foo" -> 0, "bar" -> 1, "baz" -> 42)
// )
```

The value of `confAsMap` will be:

```scala
Right(Map(foo -> 0, bar -> 1, baz -> 42))
```

<a name="valueByValueConversion"></a>
### Value-by-value conversion
Instead of using a case class you may want to retrieve the single values and convert them as you go:

```scala
val bar: Result[String] = for {
  conf <- config
  result <- conf.getAs[String]("bar")
} yield result
// bar: Result[String] = Left(
//   com.lambdista.config.ConversionError: Could not convert 1.0 to the type requested
// )
```

The value of `bar` will be:

```scala
Right("hello")
```

You can also use the *dot* syntax to retrieve a value. E.g.:

```scala
val cfgStr = """
{
  foo = {
    bar = 42
  }
}
"""
// cfgStr: String = """
// {
//   foo = {
//     bar = 42
//   }
// }
// """

val config: Result[Config] = Config.from(cfgStr)
// config: Result[Config] = Right(
//   Config(
//     AbstractMap(Map("foo" -> AbstractMap(Map("bar" -> AbstractNumber(42.0)))))
//   )
// )

val bar: Result[Int] = for {
  c <- config
  bar <- c.getAs[Int]("foo.bar")
} yield bar
// bar: Result[Int] = Right(42)
```

Note how the `bar` value was retrieved using the dot syntax.

Apart from converting the whole config into a case class, you can also convert a given value provided it's an object in
the JSON-superset syntax:

```scala
val greekList: Result[List[Greek]] = for {
  conf <- config
  result <- conf.getAs[List[Greek]]("mapList")
} yield result
// greekList: Result[List[Greek]] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: mapList
// )
```

The value of `greekList` will be:

```scala
Right(List(Greek(hello,42), Greek(world,24)))
```

Sorry? You said you would have preferred a `Vector[Greek]` in place of `List[Greek]`? No problem:

```scala
val greekVector: Result[Vector[Greek]] = for {
  conf <- config
  result <- conf.getAs[Vector[Greek]]("mapList")
} yield result
// greekVector: Result[Vector[Greek]] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: mapList
// )
```

Here's the value of `greekVector`:

```scala
Right(Vector(Greek(hello,42), Greek(world,24)))
```

Oh, yes, `Set[Greek]` would have worked too:

```scala
val greekSet: Result[Set[Greek]] = for {
  conf <- config
  result <- conf.getAs[Set[Greek]]("mapList")
} yield result
// greekSet: Result[Set[Greek]] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: mapList
// )
```

Here's the value of `greekSet`:

```scala
Right(Set(Greek(hello,42), Greek(world,24)))
```

Analogously you can automatically convert a `Range` into a `List`, `Vector` or `Set`:

```scala
val rangeAsList: Result[List[Int]] = for {
  conf <- config
  result <- conf.getAs[List[Int]]("range")
} yield result
// rangeAsList: Result[List[Int]] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: range
// )

val rangeAsVector: Result[Vector[Int]] = for {
  conf <- config
  result <- conf.getAs[Vector[Int]]("range")
} yield result
// rangeAsVector: Result[Vector[Int]] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: range
// )

val rangeAsSet: Result[Set[Int]] = for {
  conf <- config
  result <- conf.getAs[Set[Int]]("range")
} yield result
// rangeAsSet: Result[Set[Int]] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: range
// )
```

Here are the results:

```scala
Right(List(2, 4, 6, 8, 10)) // rangeAsList

Right(Vector(2, 4, 6, 8, 10)) // rangeAsVector

Right(Set(4, 2, 8, 6, 10)) // rangeAsSet
```

Notice, however, that in case of `Set` the order is not guaranteed because of the very nature of sets.

<a name="dynamicValueByValueConversion"></a>
### Dynamic value-by-value conversion
You can also use a dynamic syntax to access the configuration values by _pretending_ the `Config` object has 
those fields:

```scala
val alpha: Result[String] = for {
  conf <- config
  result <- conf.map.alpha.as[String] // equivalent to: conf.getAs[String]("map.alpha")
} yield result
// alpha: Result[String] = Left(
//   com.lambdista.config.KeyNotFoundError: No such key: map
// )
```

The value of `alpha` will be:

```scala
Right("hello")
```

**Warning**: Some IDEs could mark `map.alpha` as an error since they don't know about the dynamic nature of
those fields. Nevertheless, your code will keep compiling and working like a charm.

<a name="customDecoders"></a>
### Custom concrete value decoders
Sometimes you may want to provide a custom concrete value decoder for some configuration parameter. For example
you may want to decode a UUID as such instead of using the provided String concrete value decoder, you know,
for a better type safety.

```scala
import java.util.UUID

val confStr: String = """
  {
    uuid = "238dfdf4-850d-4643-b4f3-019252515ed8"
  }
"""
// confStr: String = """
//   {
//     uuid = "238dfdf4-850d-4643-b4f3-019252515ed8"
//   }
// """
final case class Foo(uuid: UUID)
implicit val uuidCv: ConcreteValue[UUID] = new ConcreteValue[UUID] {
  override def apply(abstractValue: AbstractValue): Option[UUID] = abstractValue match {
    case AbstractString(x) => Result.attempt(UUID.fromString(x)).toOption
    case _                 => None
  }
}
// uuidCv: ConcreteValue[UUID] = repl.Session$App$$anon$11@2015ed33

val foo: Result[Foo] = for {
  conf <- Config.from(confStr)
  result <- conf.as[Foo]
} yield result
// foo: Result[Foo] = Right(Foo(238dfdf4-850d-4643-b4f3-019252515ed8))
```

The value of `foo` will be:

```scala
Right(Foo(238dfdf4-850d-4643-b4f3-019252515ed8))
```

<a name="configLoaders"></a>
## Config loaders
Apart from loading your config through a `java.nio.file.Path` you can also use the following resources:

* String
* java.io.File
* scala.io.Source
* java.net.URI
* java.net.URL
* com.typesafe.config.Config

If that's not enough it's not so difficult provide your implementation of the `ConfigLoader` type class and make it
available in scope. Here's how the `ConfigLoader` looks like:

```scala
trait ConfigLoader[R] {
  def load(resource: R): Result[Config]
}
```

Actually all you need to do is find a way to *read* your resource into a `String` and your done. Have a look at the
`ConfigLoader` companion object for some examples.

<a name="stringLoader"></a>
### Loading config from a simple String
What follows is an example of loading the config from a simple `String`. In this example you can also appreciate
two other features of the library: how it deals with `null` values and its ability to convert char ranges too.

```scala
val confStr: String = "{age = null, charRange = 'a' to 'z'}"
// confStr: String = "{age = null, charRange = 'a' to 'z'}"
    
val config: Result[Config] = Config.from(confStr)
// config: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "age" -> AbstractNone,
//         "charRange" -> AbstractRange(
//           Range(
//             97,
//             98,
//             99,
//             100,
//             101,
//             102,
//             103,
//             104,
//             105,
//             106,
//             107,
//             108,
//             109,
//             110,
//             111,
//             112,
//             113,
//             114,
//             115,
//             116,
//             117,
//             118,
//             119,
//             120,
//             121,
//             122
//           )
//         )
//       )
//     )
//   )
// )

val age: Result[Option[Int]] = for {
  conf <- config
  result <- conf.getAs[Option[Int]]("age")
} yield result
// age: Result[Option[Int]] = Right(None)

val charRange: Result[List[Char]] = for {
  conf <- config
  result <- conf.getAs[List[Char]]("charRange")
} yield result
// charRange: Result[List[Char]] = Right(
//   List(
//     'a',
//     'b',
//     'c',
//     'd',
//     'e',
//     'f',
//     'g',
//     'h',
//     'i',
//     'j',
//     'k',
//     'l',
//     'm',
//     'n',
//     'o',
//     'p',
//     'q',
//     'r',
//     's',
//     't',
//     'u',
//     'v',
//     'w',
//     'x',
//     'y',
//     'z'
//   )
// )
```

As you may expect the values of `age` and `charRange` will be:

```scala
Right(None) // age

Right(List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z)) // charRange
```

<a name="typesafeLoader"></a>
### Loading a config from Typesafe Config
Here's how simple is loading a configuration passing through Typesafe config library. First thing first, you need to add
the dependency for the Typesafe config adapter:

```scala
libraryDependencies += "com.lambdista" %% "config-typesafe" % "0.7.1"
```

The example configuration is the following:
```
string = "hello"
int = 42
double = 1.414
boolean = true
list = [1, 2, 3]
mapList = [
  {
    firstName = "John"
    lastName = "Doe"
  }
  {
    firstName = "Jane"
    lastName = "Doe"
  }
]
```

Suppose it's in a file at the relative path `typesafe/src/test/resources/typesafe.conf`:

```scala
import java.io.File
import com.typesafe.config.{Config => TSConfig, ConfigFactory}
import com.lambdista.config.typesafe._ // important to bring into scope the ConfigLoader for Typesafe's Config // important to bring into scope the ConfigLoader for Typesafe's Config

case class Person(firstName: String, lastName: String)

case class TypesafeConfig(string: String, int: Int, double: Double, boolean: Boolean, list: List[Int], mapList: List[Person])

val confPath = "typesafe/src/test/resources/typesafe.conf"
// confPath: String = "typesafe/src/test/resources/typesafe.conf"

val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))
// tsConfig: com.typesafe.config.Config = Config(SimpleConfigObject({"boolean":true,"double":1.414,"int":42,"list":[1,2,3],"mapList":[{"firstName":"John","lastName":"Doe"},{"firstName":"Jane","lastName":"Doe"}],"string":"hello"}))

val configTry: Result[Config] = Config.from(tsConfig)
// configTry: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       HashMap(
//         "string" -> AbstractString("hello"),
//         "double" -> AbstractNumber(1.414),
//         "boolean" -> AbstractBool(true),
//         "int" -> AbstractNumber(42.0),
//         "mapList" -> AbstractList(
//           List(
//             AbstractMap(
//               Map(
//                 "firstName" -> AbstractString("John"),
//                 "lastName" -> AbstractString("Doe")
//               )
//             ),
//             AbstractMap(
//               Map(
//                 "lastName" -> AbstractString("Doe"),
//                 "firstName" -> AbstractString("Jane")
//               )
//             )
//           )
//         ),
//         "list" -> AbstractList(
//           List(AbstractNumber(1.0), AbstractNumber(2.0), AbstractNumber(3.0))
//         )
//       )
//     )
//   )
// )

val typesafeConfig: Result[TypesafeConfig] = config.flatMap(_.as[TypesafeConfig])
// typesafeConfig: Result[TypesafeConfig] = Left(
//   com.lambdista.config.ConversionError: Could not convert {age = None, charRange = [97.0, 98.0, 99.0, 100.0, 101.0, 102.0, 103.0, 104.0, 105.0, 106.0, 107.0, 108.0, 109.0, 110.0, 111.0, 112.0, 113.0, 114.0, 115.0, 116.0, 117.0, 118.0, 119.0, 120.0, 121.0, 122.0]} to the type requested
// )
```

The value of `typesafeConfig` will be:

```scala
Right(TypesafeConfig(hello,42,1.414,true,List(1, 2, 3),List(Person(John,Doe), Person(Jane,Doe))))
```

<a name="mergingConfigs"></a>
## Merging two configurations
You can also merge two configurations using either the `recursivelyMerge` or `merge` method of `Config`, 
as in `config.recursivelyMerge(thatConfig)` or `config.merge(thatConfig)`. The behaviour of the
former is that, given a key, if the correspondent value is a map then `thatConfig`'s value is
*recursively* merged to this config's value otherwise `thatConfig`'s value replaces this config's value. 
An example should clarify the difference between the two approaches:

```scala
val confStr1 = """
{
  foo = {
    alpha = 1,
    bar = "hello"
  },
  baz = 42
}
"""
// confStr1: String = """
// {
//   foo = {
//     alpha = 1,
//     bar = "hello"
//   },
//   baz = 42
// }
// """

val confStr2 = """
{
  foo = {
    baz = 15,
    bar = "goodbye"
  },
  baz = 1,
  zoo = "hi"
}
"""
// confStr2: String = """
// {
//   foo = {
//     baz = 15,
//     bar = "goodbye"
//   },
//   baz = 1,
//   zoo = "hi"
// }
// """

val config1: Result[Config] = Config.from(confStr1)
// config1: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractMap(
//           Map("alpha" -> AbstractNumber(1.0), "bar" -> AbstractString("hello"))
//         ),
//         "baz" -> AbstractNumber(42.0)
//       )
//     )
//   )
// )

val config2: Result[Config] = Config.from(confStr2)
// config2: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractMap(
//           Map("baz" -> AbstractNumber(15.0), "bar" -> AbstractString("goodbye"))
//         ),
//         "baz" -> AbstractNumber(1.0),
//         "zoo" -> AbstractString("hi")
//       )
//     )
//   )
// )

val mergedConfig: Result[Config] = for {
  conf1 <- config1
  conf2 <- config2
} yield conf1.recursivelyMerge(conf2)
// mergedConfig: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractMap(
//           Map(
//             "alpha" -> AbstractNumber(1.0),
//             "bar" -> AbstractString("goodbye"),
//             "baz" -> AbstractNumber(15.0)
//           )
//         ),
//         "baz" -> AbstractNumber(1.0),
//         "zoo" -> AbstractString("hi")
//       )
//     )
//   )
// )
```
`mergedConfig` will represent a config such as the following:
```
{
  foo = {
    alpha = 1,
    baz = 15,
    bar = "goodbye"
  },
  baz = 1,
  zoo = "hi"
}
```
As you can see the value of `config2`'s `foo` did not replace entirely the value of `config1`'s `foo`, but they
were recursively merged.

On the other hand `merge`'s behaviour is more like Scala's default behaviour when using `++` between two `Map`s. Indeed,
`config2`'s values replace entirely `config1`'s values with the same key. E.g.:
```scala
val confStr1 = """
{
  foo = {
    alpha = 1,
    bar = "hello"
  },
  zoo = "hi",
  baz = 42
}
"""
// confStr1: String = """
// {
//   foo = {
//     alpha = 1,
//     bar = "hello"
//   },
//   zoo = "hi",
//   baz = 42
// }
// """

val confStr2 = """
{
  foo = {
    baz = 15,
    bar = "goodbye"
  },
  baz = 1
}
"""
// confStr2: String = """
// {
//   foo = {
//     baz = 15,
//     bar = "goodbye"
//   },
//   baz = 1
// }
// """

val config1: Result[Config] = Config.from(confStr1)
// config1: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractMap(
//           Map("alpha" -> AbstractNumber(1.0), "bar" -> AbstractString("hello"))
//         ),
//         "zoo" -> AbstractString("hi"),
//         "baz" -> AbstractNumber(42.0)
//       )
//     )
//   )
// )

val config2: Result[Config] = Config.from(confStr2)
// config2: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractMap(
//           Map("baz" -> AbstractNumber(15.0), "bar" -> AbstractString("goodbye"))
//         ),
//         "baz" -> AbstractNumber(1.0)
//       )
//     )
//   )
// )

val mergedConfig: Result[Config] = for {
  conf1 <- config1
  conf2 <- config2
} yield conf1.merge(conf2)
// mergedConfig: Result[Config] = Right(
//   Config(
//     AbstractMap(
//       Map(
//         "foo" -> AbstractMap(
//           Map("baz" -> AbstractNumber(15.0), "bar" -> AbstractString("goodbye"))
//         ),
//         "zoo" -> AbstractString("hi"),
//         "baz" -> AbstractNumber(1.0)
//       )
//     )
//   )
// )
``` 

`mergedConfig` will represent a config such as the following:
```
{
  foo = {
    baz = 15,
    bar = "goodbye"
  },
  baz = 1,
  zoo = "hi"
}
```

Look at the tests for this library to see the examples in practise.

<a name="scaladoc"></a>
## Scaladoc API
[config API](https://javadoc.io/doc/com.lambdista/config_2.13)

<a name="feedback"></a>
## Bugs and Feedback
For bugs, questions and discussions please use [Github Issues](https://github.com/lambdista/config/issues).

<a name="license"></a>
## License
Copyright 2016-2020 Alessandro Lacava.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.