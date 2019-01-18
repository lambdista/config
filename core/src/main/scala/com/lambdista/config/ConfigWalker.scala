package com.lambdista.config

import scala.language.dynamics
import scala.util.{Failure, Try}

import com.lambdista.util.syntax.std.option._

/**
  * This class lets you walk across a configuration dynamically. Nonetheless it manages possible errors within
  * the `Try`.
  *
  * @author Alessandro Lacava (@lambdista)
  * @since 2016-11-23
  */
final case class ConfigWalker(value: Try[AbstractValue]) extends Dynamic {
  def selectDynamic(key: String): ConfigWalker = {
    val searchResult = value.flatMap {
      case x: AbstractMap => x.get(key)
      case x =>
        Failure(
          new KeyNotFoundError(s"$x is not an AbstractMap so the $key key does not make sense on this object")
        )
    }

    ConfigWalker(searchResult)
  }

  def as[A: ConcreteValue]: Try[A] =
    for {
      abstractValue <- value
      concreteValue <- ConcreteValue[A].apply(abstractValue).toTry(new ConversionError(abstractValue))
    } yield concreteValue
}
