package com.lambdista
package config

import scala.language.dynamics

/**
  * This class represents the configuration.
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
final case class Config(abstractMap: AbstractMap) extends Dynamic {

  /**
    * Tries to convert the configuration to a type for which exists an instance of
    * [[ConcreteValue]] in scope. Since the configuration is represented as a [[AbstractMap]], `A` is generally a final case class or,
    * simply, [[AbstractMap]]. The conversion to a case class happens automatically, you don't need to implement a
    * [[ConcreteValue]] instance for it. The only requirement is that the configuration keys and values match the case
    * class's field names and values, respectively. For example, given a config object pointing to this configuration:
    * {{{
    * {
    *   bar: 42,
    *   baz: "hello"
    * }
    * }}}
    * and this final case class:
    * {{{
    * final case class Foo(bar: Int, baz: String)
    * }}}
    * the conversion is possible as in:
    * {{{
    * config.as[Foo] // result Right(Foo(42, "hello"))
    * }}}
    * @return a `Result[A]`, which is a `Right` if the conversion is successful, a `Left` if it's not.
    */
  def as[A: ConcreteValue]: Result[A] = ConcreteValue[A].apply(abstractMap).toRight(new ConversionError(abstractMap))

  /**
    * Tries to retrieve a config value and convert it into a concrete Scala value. It may fail in one of two ways:
    *
    * 1. The key looked for is not found.
    *
    * 2. The conversion is not doable.
    *
    * @param key the key to look for
    * @return
    */
  def getAs[A: ConcreteValue](key: String): Result[A] = {
    getValue(key).flatMap { x =>
      ConcreteValue[A].apply(x).toRight(new ConversionError(x))
    }
  }

  /**
    * Merges two [[Config]] objects. Given a key, if the correspondent value is a map then `thatConfig`'s value is
    * "recursively" merged to this config's value otherwise `thatConfig`'s value replaces this config's value. E.g.:
    *
    * `conf1`:
    * {{{
    * {
    *	  foo = {
    *	    alpha = 1,
    *	    bar = "hello"
    *	  },
    *	  baz = 42
    * }
    * }}}
    *
    * `conf2`:
    * {{{
    * {
    *   foo = {
    *     baz = 15,
    *     bar = "goodbye"
    *   },
    *   baz = 1,
    *   zoo = "hi"
    * }
    * }}}
    * `conf1.recursivelyMerge(conf2)`:
    * {{{
    * {
    *   foo = {
    *     alpha = 1,
    *     baz = 15,
    *     bar = "goodbye"
    *   },
    *   baz = 1,
    *   zoo = "hi"
    * }
    * }}}
    *
    * @param thatConfig the [[Config]] to merge this [[Config]] with
    * @return
    */
  def recursivelyMerge(thatConfig: Config): Config = {
    Config(mergeAbstractMaps(this.abstractMap, thatConfig.abstractMap))
  }

  /**
    * Merges two [[Config]] objects. Basically if you look at the configuration as `Map`s, the resulting [[Config]] object
    * is like using `++` with the two underlying `Map`s, as in thisConfig ++ thatConfig. E.g.:
    *
    * `conf1`:
    * {{{
    * {
    *	  foo = {
    *	    alpha = 1,
    *	    bar = "hello"
    *	  },
    *	  baz = 42
    * }
    * }}}
    *
    * `conf2`:
    * {{{
    * {
    *   foo = {
    *     baz = 15,
    *     bar = "goodbye"
    *   },
    *   baz = 1,
    *   zoo = "hi"
    * }
    * }}}
    * `conf1.merge(conf2)`:
    * {{{
    * {
    *   foo = {
    *     baz = 15,
    *     bar = "goodbye"
    *   },
    *   baz = 1,
    *   zoo = "hi"
    * }
    * }}}
    *
    * @param thatConfig the [[Config]] to merge this [[Config]] with
    * @return
    */
  def merge(thatConfig: Config): Config = {
    Config(AbstractMap(this.abstractMap.value ++ thatConfig.abstractMap.value))
  }

  def selectDynamic(key: String): ConfigWalker = ConfigWalker(Right(abstractMap)).selectDynamic(key)

  private def mergeAbstractMaps(abstractMap1: AbstractMap, abstractMap2: AbstractMap): AbstractMap = {
    def mergeMaps(map1: Map[String, AbstractValue], map2: Map[String, AbstractValue]): Map[String, AbstractValue] = {
      val keys = map1.keySet ++ map2.keySet

      keys.map { key =>
        key -> ((map1.get(key), map2.get(key)) match {
          case (Some(v1: AbstractMap), Some(v2: AbstractMap)) => mergeAbstractMaps(v1, v2)
          case (Some(v1), None)                               => v1
          case (_, Some(v2))                                  => v2
          case _                                              => AbstractNone // no way! it could never end up here. I just hate match-not-exhaustive warnings. ;-)
        })
      }.toMap
    }

    AbstractMap(mergeMaps(abstractMap1.value, abstractMap2.value))
  }

  private def getValue(key: String): Result[AbstractValue] = {
    val recoverer: Result[AbstractValue] =
      if (key.exists(_ == '.'))
        getValueFromMultipleStrings(key.split("\\.").toList)
      else
        Left(new KeyNotFoundError(key))

    Result.orElse(abstractMap.get(key), recoverer)
  }

  private def getValueFromMultipleStrings(keys: List[String]): Result[AbstractValue] = {
    val head   = keys.head
    val middle = keys.tail.init
    val last   = keys.last

    val zero = abstractMap.get(head).flatMap(_.as[AbstractMap])

    val traversalResult: Result[AbstractValue] = middle.foldLeft(zero) { (acc, a) =>
      acc.flatMap(x => x.get(a).flatMap(_.as[AbstractMap]))
    }

    for {
      c <- traversalResult
      m <- c.as[AbstractMap]
      v <- m.get(last)
    } yield v
  }
}

object Config {
  val empty: Config = Config(AbstractMap(Map.empty[String, AbstractValue]))

  /**
    * Loads a configuration using the [[ConfigLoader]][R] in scope, where `R` is the resource representing
    * the configuration.
    *
    * @param resource the resource representing the configuration
    * @return a `Result[Config]`. If it's a `Left` it means that either there has been a problem loading the resource
    *         or the configuration syntax is not correct.
    */
  def from[R: ConfigLoader](resource: R): Result[Config] = ConfigLoader[R].load(resource)
}
