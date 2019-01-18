package com.lambdista
package config

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import fastparse.all._

/**
  *
  *
  * @author Alessandro Lacava (@lambdista)
  * @since 2016-10-13
  */
object ConfigParser {
  case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
    override def apply(t: T) = f(t)
    override def toString()  = name
  }

  val Whitespace        = NamedFunction(" \r\n".contains(_: Char), "Whitespace")
  val Digits            = NamedFunction('0' to '9' contains (_: Char), "Digits")
  val StringChars       = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
  val IdentifierChars   = NamedFunction(!":=/#\" ".contains(_: Char), "IdentifierChars")
  val AnyCharButEndLine = NamedFunction(!"\r\n".contains(_: Char), "AnyCharButEndLine")

  val spaces: Parser[Unit]                          = P(CharsWhile(Whitespace))
  val optionalSpaces: Parser[Unit]                  = spaces.?
  val anyCharButEndLine: Parser[Unit]               = P(CharsWhile(AnyCharButEndLine))
  val anyCharButEndOfMultilineComment: Parser[Unit] = P(StringIn("/*")) ~ P(!StringIn("*/")) ~ P(StringIn("*/"))

  val singleLineComment: Parser[Unit] = P(optionalSpaces ~ ("#" | "//") ~ anyCharButEndLine.rep ~ spaces)

  val digits: Parser[Unit]                   = P(CharsWhile(Digits))
  val exponent: Parser[Unit]                 = P(CharIn("eE") ~ CharIn("+-").? ~ digits)
  val fractional: Parser[Unit]               = P("." ~ digits)
  val integral: Parser[Unit]                 = P("0" | CharIn('1' to '9') ~ digits.?)
  val number: Parser[Double]                 = P(CharIn("+-").? ~ integral ~ fractional.? ~ exponent.?).!.map(_.toDouble)
  val abstractNumber: Parser[AbstractNumber] = number.map(AbstractNumber)

  val `null`: Parser[Unit]                    = P("null")
  val abstractNone: Parser[AbstractNone.type] = `null`.map(_ => AbstractNone)

  val `true`: Parser[Boolean]            = P("true").map(_ => true)
  val `false`: Parser[Boolean]           = P("false").map(_ => false)
  val abstractBool: Parser[AbstractBool] = (`true` | `false`).map(AbstractBool)

  val hexDigit: Parser[Unit]      = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
  val unicodeEscape: Parser[Unit] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  val escape: Parser[Unit]        = P("\\" ~ (CharIn("\"/\\bfnrt") | unicodeEscape))

  val strChars: Parser[Unit] = P(CharsWhile(StringChars))
  val idChars: Parser[Unit]  = P(CharsWhile(IdentifierChars))

  val string: Parser[String] = P(optionalSpaces ~ "\"" ~/ (strChars | escape).rep.! ~ "\"")

  val abstractString: Parser[AbstractString] = string.map(AbstractString)

  val unquotedIdentifier: Parser[String] = P((idChars | escape).rep.!)
  val quotedIdentifier: Parser[String]   = "\"" ~/ unquotedIdentifier ~ "\""

  val identifier: Parser[String] = P(optionalSpaces ~/ (quotedIdentifier | unquotedIdentifier))

  val array: Parser[Seq[AbstractValue]]  = P("[" ~/ jsonExpr.rep(sep = ",".~/) ~ optionalSpaces ~ "]")
  val abstractList: Parser[AbstractList] = array.map(xs => AbstractList(xs.toList))

  val pair: Parser[(String, AbstractValue)] = P(
    singleLineComment.rep ~ identifier ~ optionalSpaces ~ (":" | "=") ~ jsonExpr ~ singleLineComment.rep)

  val obj: Parser[Seq[(String, AbstractValue)]] = P("{" ~/ pair.rep(sep = ",".~/) ~ optionalSpaces ~ "}")
  val abstractMap: Parser[AbstractMap]          = obj.map(x => AbstractMap(x.toMap))

  object DurationDecoder {
    def apply(value: Option[Double], unit: String): Duration = {
      def buildFinite(v: Double): Duration = {
        val timeUnit: TimeUnit = unit match {
          case "days" | "day" | "d"                                       => TimeUnit.DAYS
          case "hours" | "hour" | "h"                                     => TimeUnit.HOURS
          case "minutes" | "minute" | "min"                               => TimeUnit.MINUTES
          case "seconds" | "second" | "secs" | "sec" | "s"                => TimeUnit.SECONDS
          case "milliseconds" | "millisecond" | "millis" | "milli" | "ms" => TimeUnit.MILLISECONDS
          case "microseconds" | "microsecond" | "micros" | "µs"           => TimeUnit.MICROSECONDS
          case "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns"     => TimeUnit.NANOSECONDS
        }
        Duration(v, timeUnit)
      }

      def buildInfinite(): Duration = unit match {
        case "Inf"      => Duration.Inf
        case "MinusInf" => Duration.MinusInf
      }

      value.map(buildFinite).getOrElse(buildInfinite())
    }
  }
  val validDuration = "days" | "day" | "d" |
      "hours" | "hour" | "h" |
      "minutes" | "minute" | "min" |
      "seconds" | "second" | "secs" | "sec" | "s" |
      "milliseconds" | "millisecond" | "millis" | "milli" | "ms" |
      "microseconds" | "microsecond" | "micros" | "µs" |
      "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns" |
      "Inf" | "MinusInf"
  val duration: Parser[Duration] = P((number.! ~ spaces).? ~ validDuration.!).map {
    case (value, unit) =>
      DurationDecoder(value.map(_.toDouble), unit)
  }
  val abstractDuration: Parser[AbstractDuration] = duration.map(AbstractDuration)

  def createRange(start: Int, method: String, end: Int, optStep: Option[Int]): Range = {
    val baseRange = method match {
      case "to"    => start to end
      case "until" => start until end
    }

    optStep.map(step => baseRange by step).getOrElse(baseRange)
  }
  val intRange: Parser[Range] =
    P(integral.! ~ spaces ~ ("to" | "until").! ~ spaces ~ integral.! ~ (spaces ~ "by" ~ spaces ~ integral.!).? ~ optionalSpaces).map {
      case (start, method, end, optStep) => createRange(start.toInt, method, end.toInt, optStep.map(_.toInt))
    }
  val charRange: Parser[Range] =
    P("'" ~ AnyChar.! ~ "'" ~ spaces ~ ("to" | "until").! ~ spaces ~ "'" ~ AnyChar.! ~ "'" ~ (spaces ~ "by" ~ spaces ~ integral.!).? ~ optionalSpaces).map {
      case (start, method, end, optStep) => createRange(start.head.toInt, method, end.head.toInt, optStep.map(_.toInt))
    }
  val range: Parser[Range]                 = intRange | charRange
  val abstractRange: Parser[AbstractRange] = range.map(AbstractRange)

  val jsonExpr: Parser[AbstractValue] = singleLineComment.rep ~ P(
      optionalSpaces ~ (abstractMap | abstractList | abstractDuration | abstractRange |
        abstractString | abstractBool | abstractNone | abstractNumber) ~ optionalSpaces
    ) ~ singleLineComment.rep

  def parse(confStr: String): Try[Config] = {
    jsonExpr.parse(confStr) match {
      case Parsed.Success(result, _) =>
        result match {
          case map: AbstractMap => Success(Config(map))
          case _ =>
            Failure(new ConfigSyntaxError("The whole configuration must be represented as a pseudo-json object"))
        }
      case x: Parsed.Failure => Failure(new ConfigSyntaxError(x.msg))
    }
  }
}
