[![Build Status](https://travis-ci.com/lambdista/config.svg?branch=master)](https://travis-ci.com/lambdista/config) [![codecov.io](http://codecov.io/github/lambdista/config/coverage.svg?branch=master)](http://codecov.io/github/lambdista/config?branch=master)

# config: a type safe, purely functional configuration library for Scala

## Table of Contents
* [Not only another Typesafe's config wrapper](#notOnlyTypesafeConfig)
* [Configuration Syntax](#configSyntax)
* [Usage](#usage)
    * [Automatic conversion to a case class](#caseClassConversion)
    * [Value-by-value conversion](#valueByValueConversion)
    * [Dynamic value-by-value conversion](#dynamicValueByValueConversion)
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
[HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) doesn't let you do it.
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
As a first step you need to add the dependency to your build file:

```scala
libraryDependencies += "com.lambdista" %% "config" % "0.5.2"
```

Scala 2.12.x, 2.11.x and 2.10.x are supported.

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

```tut:silent
import scala.util.Try
import scala.concurrent.duration.Duration

import java.nio.file.Paths

import com.lambdista.config._

val confPath = "core/src/test/resources/foo.conf"

val config: Try[Config] = Config.from(Paths.get(confPath))
```

Apart from `java.nio.file.Path` you can load your config from other resources using [Config Loaders](#configLoaders).
 
As you can see the result is a `Try[Config]`. Indeed you can get two types of error here:

* The resource cannot be found.
* The resource can be found but its parsing failed.

In both cases you would get a `Failure` wrapping the appropriate exception.

Once you have a `Config` object you can do two main things with it:

* Convert it entirely into a case class representing the whole configuration.
* Retrieve a single value and convert it to whatever it's convertible to.

<a name="caseClassConversion"></a>
### Automatic conversion to a case class
Here's how you would map the previous configuration to a case class (`config` is the value from the previous example):

```tut:silent
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

val fooConfig: Try[FooConfig] = for {
  conf <- config
  result <- conf.as[FooConfig]
} yield result
```

The value of `fooConfig` will be:

```scala
Success(FooConfig(hello,Some(42),List(1, 2, 3),Greek(hello,42),List(Greek(hello,42), Greek(world,24)),Range(2, 4, 6, 8, 10),5 seconds))
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

<a name="valueByValueConversion"></a>
### Value-by-value conversion
Instead of using a case class you may want to retrieve the single values and convert them as you go:

```tut:silent
val bar: Try[String] = for {
  conf <- config
  result <- conf.getAs[String]("bar")
} yield result
```

The value of `bar` will be:

```scala
Success("hello")
```

You can also use the *dot* syntax to retrieve a value. E.g.:

```tut:silent
val cfgStr = """
{
  foo = {
    bar = 42
  }
}
"""

val config: Try[Config] = Config.from(cfgStr)

val bar: Try[Int] = for {
  c <- config
  bar <- c.getAs[Int]("foo.bar")
} yield bar
```

Note how the `bar` value was retrieved using the dot syntax.

Apart from converting the whole config into a case class, you can also convert a given value provided it's an object in
the JSON-superset syntax:

```tut:silent
val greekList: Try[List[Greek]] = for {
  conf <- config
  result <- conf.getAs[List[Greek]]("mapList")
} yield result
```

The value of `greekList` will be:

```scala
Success(List(Greek(hello,42), Greek(world,24)))
```

Sorry? You said you would have preferred a `Vector[Greek]` in place of `List[Greek]`? No problem:

```tut:silent
val greekVector: Try[Vector[Greek]] = for {
  conf <- config
  result <- conf.getAs[Vector[Greek]]("mapList")
} yield result
```

Here's the value of `greekVector`:

```scala
Success(Vector(Greek(hello,42), Greek(world,24)))
```

Oh, yes, `Set[Greek]` would have worked too:

```tut:silent
val greekSet: Try[Set[Greek]] = for {
  conf <- config
  result <- conf.getAs[Set[Greek]]("mapList")
} yield result
```

Here's the value of `greekSet`:

```scala
Success(Set(Greek(hello,42), Greek(world,24)))
```

Analogously you can automatically convert a `Range` into a `List`, `Vector` or `Set`:

```tut:silent
val rangeAsList: Try[List[Int]] = for {
  conf <- config
  result <- conf.getAs[List[Int]]("range")
} yield result

val rangeAsVector: Try[Vector[Int]] = for {
  conf <- config
  result <- conf.getAs[Vector[Int]]("range")
} yield result

val rangeAsSet: Try[Set[Int]] = for {
  conf <- config
  result <- conf.getAs[Set[Int]]("range")
} yield result
```

Here are the results:

```scala
Success(List(2, 4, 6, 8, 10)) // rangeAsList

Success(Vector(2, 4, 6, 8, 10)) // rangeAsVector

Success(Set(4, 2, 8, 6, 10)) // rangeAsSet
```

Notice, however, that in case of `Set` the order is not guaranteed because of the very nature of sets.

<a name="dynamicValueByValueConversion"></a>
### Dynamic value-by-value conversion
You can also use a dynamic syntax to access the configuration values by _pretending_ the `Config` object has 
those fields:

```tut:silent
val alpha: Try[String] = for {
  conf <- config
  result <- conf.map.alpha.as[String] // equivalent to: conf.getAs[String]("map.alpha")
} yield result
```

The value of `alpha` will be:

```scala
Success("hello")
```

**Warning**: Some IDEs could mark `map.alpha` as an error since they don't know about the dynamic nature of
those fields. Nevertheless, your code will keep compiling and working like a charm.

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
  def load(resource: R): Try[Config]
}
```

Actually all you need to do is find a way to *read* your resource into a `String` and your done. Have a look at the
`ConfigLoader` companion object for some examples.

<a name="stringLoader"></a>
### Loading config from a simple String
What follows is an example of loading the config from a simple `String`. In this example you can also appreciate
two other features of the library: how it deals with `null` values and its ability to convert char ranges too.

```tut:silent
val confStr: String = "{age = null, charRange = 'a' to 'z'}"
    
val config: Try[Config] = Config.from(confStr)

val age: Try[Option[Int]] = for {
  conf <- config
  result <- conf.getAs[Option[Int]]("age")
} yield result

val age: Try[List[Char]] = for {
  conf <- config
  result <- conf.getAs[List[Char]]("charRange")
} yield result
```

As you may expect the values of `age` and `charRange` will be:

```scala
Success(None) // age

Success(List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z)) // charRange
```

<a name="typesafeLoader"></a>
### Loading a config from Typesafe Config
Here's how simple is loading a configuration passing through Typesafe config library. First thing first, you need to add
the dependency for the Typesafe config adapter:

```scala
libraryDependencies += "com.lambdista" %% "config-typesafe" % "0.5.2"
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

```tut:silent
import scala.util.Try

import java.io.File
import com.typesafe.config.{Config => TSConfig, ConfigFactory}
import com.lambdista.config.typesafe._ // important to bring into scope the ConfigLoader for Typesafe's Config

case class Person(firstName: String, lastName: String)

case class TypesafeConfig(string: String, int: Int, double: Double, boolean: Boolean, list: List[Int], mapList: List[Person])

val confPath = "typesafe/src/test/resources/typesafe.conf"

val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

val configTry: Try[Config] = Config.from(tsConfig)

val typesafeConfig: Try[TypesafeConfig] = config.flatMap(_.as[TypesafeConfig])
```

The value of `typesafeConfig` will be:

```scala
Success(TypesafeConfig(hello,42,1.414,true,List(1, 2, 3),List(Person(John,Doe), Person(Jane,Doe))))
```

<a name="mergingConfigs"></a>
## Merging two configurations
You can also merge two configurations using either the `recursivelyMerge` or `merge` method of `Config`, 
as in `config.recursivelyMerge(thatConfig)` or `config.merge(thatConfig)`. The behaviour of the
former is that, given a key, if the correspondent value is a map then `thatConfig`'s value is
*recursively* merged to this config's value otherwise `thatConfig`'s value replaces this config's value. 
An example should clarify the difference between the two approaches:

```tut:silent
val confStr1 = """
{
  foo = {
    alpha = 1,
    bar = "hello"
  },
  baz = 42
}
"""

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

val config1: Try[Config] = Config.from(confStr1)

val config2: Try[Config] = Config.from(confStr2)

val mergedConfig: Try[Config] = for {
  conf1 <- config1
  conf2 <- config2
} yield conf1.recursivelyMerge(conf2)

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
```tut:silent
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

val confStr2 = """
{
  foo = {
    baz = 15,
    bar = "goodbye"
  },
  baz = 1
}
"""

val config1: Try[Config] = Config.from(confStr1)

val config2: Try[Config] = Config.from(confStr2)

val mergedConfig: Try[Config] = for {
  conf1 <- config1
  conf2 <- config2
} yield conf1.merge(conf2)
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
[config API](https://javadoc.io/doc/com.lambdista/config_2.12)

<a name="feedback"></a>
## Bugs and Feedback
For bugs, questions and discussions please use [Github Issues](https://github.com/lambdista/config/issues).

<a name="license"></a>
## License
Copyright 2016 Alessandro Lacava.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.