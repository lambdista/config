package com.lambdista
package config

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import fastparse.all._

import com.lambdista.config.exception.ConfigSyntaxException

/**
  *
  *
  * @author Alessandro Lacava (@lambdista)
  * @since 2016-10-13
  */
object ConfParser {
  case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
    def apply(t: T)         = f(t)
    override def toString() = name

  }

  val Whitespace      = NamedFunction(" \r\n".contains(_: Char), "Whitespace")
  val Digits          = NamedFunction('0' to '9' contains (_: Char), "Digits")
  val StringChars     = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
  val IdentifierChars = NamedFunction(!":= ".contains(_: Char), "IdentifierChars")

  val optionalSpaces = P(CharsWhile(Whitespace).?)
  val spaces         = P(CharsWhile(Whitespace))
  val digits         = P(CharsWhile(Digits))
  val exponent       = P(CharIn("eE") ~ CharIn("+-").? ~ digits)
  val fractional     = P("." ~ digits)
  val integral       = P("0" | CharIn('1' to '9') ~ digits.?)

  val number: Parser[Double] = P(CharIn("+-").? ~ integral ~ fractional.? ~ exponent.?).!.map(_.toDouble)
  val abstractNumber         = number.map(AbstractNumber)

  val `null`: Parser[AbstractNone.type] = P("null").map(_ => AbstractNone)
  val `false`: Parser[AbstractBool]     = P("false").map(_ => AbstractBool(false))
  val `true`: Parser[AbstractBool]      = P("true").map(_ => AbstractBool(true))

  val hexDigit      = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
  val unicodeEscape = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  val escape        = P("\\" ~ (CharIn("\"/\\bfnrt") | unicodeEscape))

  val strChars = P(CharsWhile(StringChars))
  val idChars  = P(CharsWhile(IdentifierChars))

  val string: Parser[String] = P(optionalSpaces ~ "\"" ~/ (strChars | escape).rep.! ~ "\"")

  val abstractString: Parser[AbstractString] = string.map(AbstractString)

  val freeIdentifier: Parser[String] = P(optionalSpaces ~/ (idChars | escape).rep.!)

  val identifier: Parser[String] = string | freeIdentifier

  val array: Parser[AbstractList] =
    P("[" ~/ jsonExpr.rep(sep = ",".~/) ~ optionalSpaces ~ "]").map(xs => AbstractList(xs.toList))

  val pair = P(identifier ~/ optionalSpaces ~ (":" | "=") ~/ jsonExpr)

  val obj: Parser[AbstractMap] =
    P("{" ~/ pair.rep(sep = ",".~/) ~ optionalSpaces ~ "}").map(x => AbstractMap(x.toMap))

  object DurationDecoder {
    def apply(value: Double, unit: String): Duration = {
      val timeUnit: TimeUnit = unit match {
        case "days" | "day" | "d"                                       => TimeUnit.DAYS
        case "hours" | "hour" | "h"                                     => TimeUnit.HOURS
        case "minutes" | "minute" | "min"                               => TimeUnit.MINUTES
        case "seconds" | "second" | "secs" | "sec" | "s"                => TimeUnit.SECONDS
        case "milliseconds" | "millisecond" | "millis" | "milli" | "ms" => TimeUnit.MILLISECONDS
        case "microseconds" | "microsecond" | "micros" | "µs"           => TimeUnit.MICROSECONDS
        case "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns"     => TimeUnit.NANOSECONDS
        //      case "Inf" | "Inf" => TimeUnit
        case _ => throw new ConfigSyntaxException("Could not recognize time unit in duration")
      }

      Duration(value, timeUnit)
    }
  }
  val validDuration = "days" | "day" | "d" |
      "hours" | "hour" | "h" |
      "minutes" | "minute" | "min" |
      "seconds" | "second" | "secs" | "sec" | "s" |
      "milliseconds" | "millisecond" | "millis" | "milli" | "ms" |
      "microseconds" | "microsecond" | "micros" | "µs" |
      "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns"

  val duration: Parser[Duration] = P(number.! ~ spaces ~ validDuration.!).map {
    case (value, unit) =>
      DurationDecoder(value.toDouble, unit)
  }
  val abstractDuration: Parser[AbstractDuration] = duration.map(AbstractDuration)

  val jsonExpr: Parser[AbstractValue] = P(
    optionalSpaces ~ (obj | array | abstractDuration | abstractString | `true` | `false` | `null` | abstractNumber) ~ optionalSpaces
  )

  def parse(confStr: String): Try[Config] = {
    jsonExpr.parse(confStr) match {
      case Parsed.Success(result, _) =>
        result match {
          case map: AbstractMap => Success(Config(map))
          case _                => Failure(new ConfigSyntaxException("The whole configuration must be represented as a pseudo-Map"))
        }
      case x: Parsed.Failure => Failure(new ConfigSyntaxException(x.msg))
    }
  }
}

object ConfParserTest {
  case class Conf(omg: String, wtf: Double, duration: Duration)

  def main(args: Array[String]): Unit = {
    val result: Try[Config] = ConfParser.parse(
      """{omg   = "123", "wtf": 12.4123, duration = 5 seconds}"""
    )

    val conf = result.flatMap(_.as[Conf])

    println(s"result: $result")
    println(s"conf: $conf")

  }
}
