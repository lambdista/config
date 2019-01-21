package com.lambdista
package config

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import fastparse._, NoWhitespace._

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

  def spaces[_: P]: P[Unit]                          = P(CharsWhile(Whitespace))
  def optionalSpaces[_: P]: P[Unit]                  = spaces.?
  def anyCharButEndLine[_: P]: P[Unit]               = P(CharsWhile(AnyCharButEndLine))
  def anyCharButEndOfMultilineComment[_: P]: P[Unit] = P(StringIn("/*")) ~ P(!StringIn("*/")) ~ P(StringIn("*/"))

  def singleLineComment[_: P]: P[Unit] = P(optionalSpaces ~ ("#" | "//") ~ anyCharButEndLine.rep ~ spaces)

  def digits[_: P]: P[Unit]                   = P(CharsWhile(Digits))
  def exponent[_: P]: P[Unit]                 = P(CharIn("eE") ~ CharIn("+\\-").? ~ digits)
  def fractional[_: P]: P[Unit]               = P("." ~ digits)
  def integral[_: P]: P[Unit]                 = P("0" | CharIn("1-9") ~ digits.?)
  def number[_: P]: P[Double]                 = P(CharIn("+\\-").? ~ integral ~ fractional.? ~ exponent.?).!.map(_.toDouble)
  def abstractNumber[_: P]: P[AbstractNumber] = number.map(AbstractNumber)

  def `null`[_: P]: P[Unit]                    = P("null")
  def abstractNone[_: P]: P[AbstractNone.type] = `null`.map(_ => AbstractNone)

  def `true`[_: P]: P[Boolean]            = P("true").map(_ => true)
  def `false`[_: P]: P[Boolean]           = P("false").map(_ => false)
  def abstractBool[_: P]: P[AbstractBool] = (`true` | `false`).map(AbstractBool)

  def hexDigit[_: P]: P[Unit]      = P(CharIn("0-9a-fA-F"))
  def unicodeEscape[_: P]: P[Unit] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  def escape[_: P]                 = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  def strChars[_: P]: P[Unit] = P(CharsWhile(StringChars))
  def idChars[_: P]: P[Unit]  = P(CharsWhile(IdentifierChars))

  def string[_: P]: P[String] = P(optionalSpaces ~ "\"" ~/ (strChars | escape).rep.! ~ "\"")

  def abstractString[_: P]: P[AbstractString] = string.map(AbstractString)

  def unquotedIdentifier[_: P]: P[String] = P((idChars | escape).rep.!)
  def quotedIdentifier[_: P]: P[String]   = "\"" ~/ unquotedIdentifier ~ "\""

  def identifier[_: P]: P[String] = P(optionalSpaces ~/ (quotedIdentifier | unquotedIdentifier))

  def array[_: P]: P[Seq[AbstractValue]]  = P("[" ~/ jsonExpr.rep(sep = ","./) ~ optionalSpaces ~ "]")
  def abstractList[_: P]: P[AbstractList] = array.map(xs => AbstractList(xs.toList))

  def pair[_: P]: P[(String, AbstractValue)] =
    P(singleLineComment.rep ~ identifier ~ optionalSpaces ~ (":" | "=") ~ jsonExpr ~ singleLineComment.rep)

  def obj[_: P]: P[Seq[(String, AbstractValue)]] = P("{" ~/ pair.rep(sep = ","./) ~ optionalSpaces ~ "}")
  def abstractMap[_: P]: P[AbstractMap]          = obj.map(x => AbstractMap(x.toMap))

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
  def validDuration[_: P] =
    "days" | "day" | "d" |
      "hours" | "hour" | "h" |
      "minutes" | "minute" | "min" |
      "seconds" | "second" | "secs" | "sec" | "s" |
      "milliseconds" | "millisecond" | "millis" | "milli" | "ms" |
      "microseconds" | "microsecond" | "micros" | "µs" |
      "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns" |
      "Inf" | "MinusInf"
  def duration[_: P]: P[Duration] = P((number.! ~ spaces).? ~ validDuration.!).map {
    case (value, unit) =>
      DurationDecoder(value.map(_.toDouble), unit)
  }
  def abstractDuration[_: P]: P[AbstractDuration] = duration.map(AbstractDuration)

  def createRange(start: Int, method: String, end: Int, optStep: Option[Int]): Range = {
    val baseRange = method match {
      case "to"    => start to end
      case "until" => start until end
    }

    optStep.map(step => baseRange by step).getOrElse(baseRange)
  }
  def intRange[_: P]: P[Range] =
    P(
      integral.! ~ spaces ~ ("to" | "until").! ~ spaces ~ integral.! ~ (spaces ~ "by" ~ spaces ~ integral.!).? ~ optionalSpaces
    ).map {
      case (start, method, end, optStep) => createRange(start.toInt, method, end.toInt, optStep.map(_.toInt))
    }
  def charRange[_: P]: P[Range] =
    P(
      "'" ~ AnyChar.! ~ "'" ~ spaces ~ ("to" | "until").! ~ spaces ~ "'" ~ AnyChar.! ~ "'" ~ (spaces ~ "by" ~ spaces ~ integral.!).? ~ optionalSpaces
    ).map {
      case (start, method, end, optStep) => createRange(start.head.toInt, method, end.head.toInt, optStep.map(_.toInt))
    }
  def range[_: P]: P[Range]                 = intRange | charRange
  def abstractRange[_: P]: P[AbstractRange] = range.map(AbstractRange)

  def jsonExpr[_: P]: P[AbstractValue] =
    singleLineComment.rep ~ P(
      optionalSpaces ~ (abstractMap | abstractList | abstractDuration | abstractRange |
        abstractString | abstractBool | abstractNone | abstractNumber) ~ optionalSpaces
    ) ~ singleLineComment.rep

  def parse(confStr: String): Try[Config] = {
    fastparse.parse(confStr, jsonExpr(_)) match {
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
