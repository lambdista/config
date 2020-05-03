package com.lambdista
package config

import scala.concurrent.duration.Duration
import scala.util.Try

import com.lambdista.util.syntax.std.option._

/**
  * The abstract config value.
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
sealed trait AbstractValue {

  /**
    * Tries to convert this [[AbstractValue]] into an `A`, provided that there's an implicit
    * implementation of [[ConcreteValue]][A] in scope.
    *
    * @return a `Try[A]`
    */
  def as[A: ConcreteValue]: Try[A] = ConcreteValue[A].apply(this).toTry(new ConversionError(this))

  /**
    * Description of this [[AbstractValue]].
    *
    * @return a `String` representing a description for this [[AbstractValue]]
    */
  def describe: String
}

final case class AbstractBool(value: Boolean) extends AbstractValue {
  override def describe: String = value.toString
}

final case class AbstractNumber(value: Double) extends AbstractValue {
  override def describe: String = value.toString
}

final case class AbstractChar(value: Char) extends AbstractValue {
  override def describe: String = s"'${value.toString}'"
}

final case class AbstractString(value: String) extends AbstractValue {
  override def describe: String = s""""$value""""
}

final case class AbstractDuration(value: Duration) extends AbstractValue {
  override def describe: String = value.toString
}

final case class AbstractRange(value: Range) extends AbstractValue {
  override def describe: String = AbstractList(value.toList.map(x => AbstractNumber(x.toDouble))).describe
}

case object AbstractNone extends AbstractValue {
  override def describe: String = "None"
}

final case class AbstractList(values: List[AbstractValue]) extends AbstractValue {
  override def describe: String = {
    val s = values.map(_.describe).mkString(", ")
    s"[$s]"
  }
}

final case class AbstractMap(value: Map[String, AbstractValue]) extends AbstractValue {
  override def describe: String = {
    val s = value.map { case (k, v) => s"$k = ${v.describe}" }.mkString(", ")
    s"{$s}"
  }

  def get(key: String): Try[AbstractValue] = value.get(key).toTry(new KeyNotFoundError(key))

  def transformKeys(f: PartialFunction[String, String]): AbstractMap =
    AbstractMap(value.map {
      case (k, v) => f.applyOrElse(k, identity[String]) -> v
    })
}
