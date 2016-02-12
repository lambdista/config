# config: a type safe, purely functional configuration library for Scala

## Not only another Typesafe's config wrapper
Right from the start I didn't want to depend on other config libraries when I started implementing this one so I wrote
my own parser for a simple JSON-like syntax.
Hence, this is not just another [Typesafe's config](https://github.com/typesafehub/config) wrapper. Of course,
if you are already using Typesafe's config library and/or just
prefer [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) syntax for your configuration,
there's an adapter that will convert a Typesafe `Config` object into this config's AST.
See [this example](#typesafeConfig).

## Configuration Syntax
The syntax expected by this library is a JSON-superset. This means that any JSON file
should be a valid configuration. However, the `null` JSON values can only be converted to `Option[A]`, where `A` 
is the type you expect, because, of course, we don't fancy `null` in Scala code. The *superset* part means that:
  
* You can optionally use `=` instead of `:`
* You can avoid putting the keys between quotes, unless your key contains white spaces
* You can use a Scala [Duration](http://www.scala-lang.org/api/current/index.html#scala.concurrent.duration.Duration)
* You can use a Scala [Range](http://www.scala-lang.org/api/current/index.html#scala.collection.immutable.Range)

## Using config
As a first step you need to add the dependency to your build file:

```scala
libraryDependencies += "com.lambdista" %% "config" % configVersion
```

Note that `configVersion` is the version you want to use. 
You can find all the released versions [here](https://github.com/lambdista/config/releases).

At the moment only Scala 2.11.x is supported. Support for Scala 2.10.x might be added in future releases.

## Usage
Ok, let's see the typical usage scenarios. As a use case consider the following configuration, unless otherwise specified:

```
{
  bar = "hello",
  baz = 42,
  list = [1, 2, 3],
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

Suppose the previous configuration is at the relative path will be: `core/src/test/resources/foo.conf`.

You can move around a config choosing one of two approaches: *functional* and *imperative*.
In the following examples you'll see both.

### Functional Approach
It consists in moving aroung the config through `map` and `flatMap` of the `Try` type, or, analogously, through
the `for expression`. Here are some examples.

First thing first, load and parse your config:

```scala
import scala.util.Try

import java.nio.file.Paths

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

#### Conversion to a case class
Here's how you would map the `foo.conf` file to a case class:

```scala
case class Greek(alpha: String, beta: Int)

case class FooConfig(bar: String, baz: Option[Int], list: List[Int], mapList: List[Greek], range: Range, duration: Duration)

val fooConfig: Try[FooConfig] = for {
  conf <- config
  result <- conf.tryAs[FooConfig]
} yield result
```

The value of `fooConfig` will be:

```scala
Success(FooConfig(hello,Some(42),List(1, 2, 3),List(Greek(hello,42), Greek(world,24)),Range(2, 4, 6, 8, 10),5 seconds))
```

Here you can already notice some interesting features of this library:

* The conversion to a case class happens automatically, no boilerplate on the client side is required.
* Since `baz` is declared as `Option[Int]` the library automatically wraps the `Int` value into a `Some`.
* The automatic conversion works also for nested structures, see `mapList` for example.
* `Range` and `Duration` work like a charm. Note that for both `Range` and `Duration` you can use the syntax you
would use in regular Scala code. For example, you could have used `5 secs` instead of `5 seconds` in `foo.conf` and
it would have worked smoothly.

#### Retrieve and convert a single value
Instead of using a case class you may want to retrieve the single values and convert them as you go:

```scala
val bar: Try[String] = for {
  conf <- config
  result <- conf.tryGet[String]("bar")
} yield result
```

The value of `bar` will be:

```scala
Success("hello")
```

You can also use the *dot* syntax to retrieve a value. For example suppose you have the following configuration:

```
{
  foo = {
    bar = 42
  }
}
```

Once loaded you could retrieve the `bar` value as follows:

```scala
// val config: Try[Config] = ...

val bar: Try[Int] = for {
  c <- config
  bar <- c.tryGet[Int]("foo.bar")
} yield bar
```

Apart from converting the whole config into a case class, you can also convert a given value provided it's an object in
the JSON-superset syntax:

```scala
val greekList: Try[List[Greek]] = for {
  conf <- config
  result <- conf.tryGet[List[Greek]]("mapList")
} yield result
```

The value of `greekList` will be:

```scala
Success(List(Greek(hello,42), Greek(world,24)))
```

Sorry? You said you would have preferred a `Vector[Greek]` in place of `List[Greek]`? No problem:

```scala
val greekVector: Try[Vector[Greek]] = for {
  conf <- config
  result <- conf.tryGet[Vector[Greek]]("mapList")
} yield result
```

Here's the value of `greekVector`:

```scala
Success(Vector(Greek(hello,42), Greek(world,24)))
```

Oh, yes, `Set[Greek]` would have worked too:

```scala
val greekSet: Try[Set[Greek]] = for {
  conf <- config
  result <- conf.tryGet[Set[Greek]]("mapList")
} yield result
```

Here's the value of `greekSet`:

```scala
Success(Set(Greek(hello,42), Greek(world,24)))
```

Analogously you can automatically convert a `Range` into a `List`, `Vector` or `Set`:

```scala
val rangeAsList: Try[List[Int]] = for {
  conf <- config
  result <- conf.tryGet[List[Int]]("range")
} yield result

val rangeAsVector: Try[Vector[Int]] = for {
  conf <- config
  result <- conf.tryGet[Vector[Int]]("range")
} yield result

val rangeAsSet: Try[Set[Int]] = for {
  conf <- config
  result <- conf.tryGet[Set[Int]]("range")
} yield result
```

Here are the results:

```scala
Success(List(2, 4, 6, 8, 10)) // rangeAsList

Success(Vector(2, 4, 6, 8, 10)) // rangeAsVector

Success(Set(4, 2, 8, 6, 10)) // rangeAsSet
```

Notice, however, that in case of `Set` the order is not guaranteed because of the very nature of sets.

### Imperative Approach
If you feel *confident* and prefer not to move within a `Try` you can opt for the *imperative* fashion. In this case, 
if the config element is not found or cannot be converted into the desired type an exception is thrown.
Here's an example:

```scala
val config: Config = Config.from(Paths.get(confPath)).get // calling .get you "get out" of the Try type

val fooConfig: FooConfig = config.as[FooConfig] // conversion to a case class...

// ...or just retrieve the config elements one by one as in the following cases
val bar: String = config.get[String]("bar")
val range: Range = config.get[Range]("range")
```

Of course you could also retrieve optional values from config and fall back on defaults in case they're missing:

```scala
val age: Int = config.tryGet[Int]("age").getOrElse(42) // result -> 42
```

<a name="configLoaders"></a>
### Config Loaders
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

#### Loading config from a simple String
What follows is an example of loading the config from a simple `String`. In this example you can also appreciate
two other features of the library: how it deals with `null` values and its ability to convert char ranges too.

```scala
val confStr: String = "{age = null, charRange = 'a' to 'z'}"
    
val config: Config = Config.from(confStr).get

val age: Option[Int] = config.get[Option[Int]]("age")

val charRange: List[Char] = config.get[List[Char]]("charRange")
```

As you may expect the values of `age` and `charRange` will be:

```scala
None // age

List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z) // charRange
```

<a name="typesafeConfig"></a>
#### Loading a config from Typesafe Config
Here's how simple is loading a configuration passing through Typesafe config library. First thing first, you need to add
the dependency for the Typesafe config adapter:

```scala
libraryDependencies += "com.lambdista" %% "config-typesafe" % configVersion
```

`configVersion` is the same you used for the core library.

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
import scala.util.Try

import java.io.File
import com.typesafe.config.{Config => TSConfig, ConfigFactory}
import com.lambdista.config.typesafe._ // important to bring into scope the ConfigLoader for Typesafe's Config

case class Person(firstName: String, lastName: String)

case class TypesafeConfig(string: String, int: Int, double: Double, boolean: Boolean, list: List[Int], mapList: List[Person])

val confPath = "typesafe/src/test/resources/typesafe.conf"

val tsConfig: TSConfig = ConfigFactory.parseFile(new File(confPath))

val configTry: Try[Config] = Config.from(tsConfig)

val typesafeConfigTry: Try[TypesafeConfig] = config.flatMap(_.tryAs[TypesafeConfig])

// or, using the "imperative" way, outside the Try
val config: Config = Config.from(tsConfig).get

val typesafeConfig: TypesafeConfig = config.as[TypesafeConfig]
```

The value of `typesafeConfig` and `typesafeConfigTry` will be:

```scala
TypesafeConfig(hello,42,1.414,true,List(1, 2, 3),List(Person(John,Doe), Person(Jane,Doe))) // typesafeConfig

Success(TypesafeConfig(hello,42,1.414,true,List(1, 2, 3),List(Person(John,Doe), Person(Jane,Doe)))) // typesafeConfigTry
```

### Merging two configurations
You can also merge two configurations using the `softMerge` or `hardMerge` methods of `Config`, 
as in `config.softMerge(thatConfig)` or `config.hardMerge(thatConfig)`. The behaviour of the
former is that, given a key, if the correspondent value is a map then `thatConfig`'s value is
*softly* merged to this config's value otherwise `thatConfig`'s value replaces this config's value. An example should
clarify it:

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

val config1: Config = Config.from(confStr1).get

val config2: Config = Config.from(confStr2).get

val mergedConfig: Config = config1.softMerge(config2)
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
were *softly* merged.

On the other hand `hardMerge`'s behaviour is more like Scala's default behaviour when using `++` between two `Map`s and
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

val confStr2 = """
{
  foo = {
    baz = 15,
    bar = "goodbye"
  },
  baz = 1
}
"""

val config1: Config = Config.from(confStr1).get

val config2: Config = Config.from(confStr2).get

val mergedConfig: Config = config1.softMerge(config2)
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

## Bugs and Feedback
For bugs, questions and discussions please use [Github Issues](https://github.com/lambdista/config/issues).

## License
Copyright 2016 Alessandro Lacava.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.