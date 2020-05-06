package com.lambdista.config

import scala.language.dynamics

/**
  * This class lets you walk across a configuration dynamically. Nonetheless it manages possible errors within
  * [[com.lambdista.config.Result]].
  *
  * @author Alessandro Lacava (@lambdista)
  * @since 2016-11-23
  */
final case class ConfigWalker(value: Result[AbstractValue]) extends Dynamic {
  def selectDynamic(key: String): ConfigWalker = {
    val searchResult = value.flatMap {
      case x: AbstractMap => x.get(key)
      case x =>
        Left(
          new KeyNotFoundError(s"$x is not an AbstractMap so the $key key does not make sense on this object")
        )
    }

    ConfigWalker(searchResult)
  }

  def as[A: ConcreteValue]: Result[A] =
    for {
      abstractValue <- value
      concreteValue <- ConcreteValue[A].apply(abstractValue).toRight(new ConversionError(abstractValue))
    } yield concreteValue
}
