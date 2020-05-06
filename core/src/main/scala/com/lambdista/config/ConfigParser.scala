package com.lambdista
package config

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

import fastparse.NoWhitespace._
import fastparse._

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

  private val Whitespace        = NamedFunction(" \r\n".contains(_: Char), "Whitespace")
  private val Digits            = NamedFunction('0' to '9' contains (_: Char), "Digits")
  private val StringChars       = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
  private val IdentifierChars   = NamedFunction(!":=/#\" ".contains(_: Char), "IdentifierChars")
  private val AnyCharButEndLine = NamedFunction(!"\r\n".contains(_: Char), "AnyCharButEndLine")

  private def spaces[_: P]: P[Unit]            = P(CharsWhile(Whitespace))
  private def optionalSpaces[_: P]: P[Unit]    = spaces.?
  private def anyCharButEndLine[_: P]: P[Unit] = P(CharsWhile(AnyCharButEndLine))

  private def singleLineComment[_: P]: P[Unit] = P(optionalSpaces ~ ("#" | "//") ~ anyCharButEndLine.rep ~ spaces)

  private def digits[_: P]: P[Unit]                   = P(CharsWhile(Digits))
  private def exponent[_: P]: P[Unit]                 = P(CharIn("eE") ~ CharIn("+\\-").? ~ digits)
  private def fractional[_: P]: P[Unit]               = P("." ~ digits)
  private def integral[_: P]: P[Unit]                 = P("0" | CharIn("1-9") ~ digits.?)
  private def number[_: P]: P[Double]                 = P(CharIn("+\\-").? ~ integral ~ fractional.? ~ exponent.?).!.map(_.toDouble)
  private def abstractNumber[_: P]: P[AbstractNumber] = number.map(AbstractNumber)

  private def `null`[_: P]: P[Unit]                    = P("null")
  private def abstractNone[_: P]: P[AbstractNone.type] = `null`.map(_ => AbstractNone)

  private def `true`[_: P]: P[Boolean]            = P("true").map(_ => true)
  private def `false`[_: P]: P[Boolean]           = P("false").map(_ => false)
  private def abstractBool[_: P]: P[AbstractBool] = (`true` | `false`).map(AbstractBool)

  private def hexDigit[_: P]: P[Unit]      = P(CharIn("0-9a-fA-F"))
  private def unicodeEscape[_: P]: P[Unit] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  private def escape[_: P]                 = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  private def strChars[_: P]: P[Unit] = P(CharsWhile(StringChars))
  private def idChars[_: P]: P[Unit]  = P(CharsWhile(IdentifierChars))

  private def string[_: P]: P[String] = P(optionalSpaces ~ "\"" ~/ (strChars | escape).rep.! ~ "\"")

  private def abstractString[_: P]: P[AbstractString] = string.map(AbstractString)

  private def unquotedIdentifier[_: P]: P[String] = P((idChars | escape).rep.!)
  private def quotedIdentifier[_: P]: P[String]   = "\"" ~/ unquotedIdentifier ~ "\""

  private def identifier[_: P]: P[String] = P(optionalSpaces ~/ (quotedIdentifier | unquotedIdentifier))

  private def array[_: P]: P[Seq[AbstractValue]]  = P("[" ~/ jsonExpr.rep(sep = ","./) ~ optionalSpaces ~ "]")
  private def abstractList[_: P]: P[AbstractList] = array.map(xs => AbstractList(xs.toList))

  private def pair[_: P]: P[(String, AbstractValue)] =
    P(singleLineComment.rep ~ identifier ~ optionalSpaces ~ (":" | "=") ~ jsonExpr ~ singleLineComment.rep)

  private def obj[_: P]: P[Seq[(String, AbstractValue)]] = P("{" ~/ pair.rep(sep = ","./) ~ optionalSpaces ~ "}")
  private def abstractMap[_: P]: P[AbstractMap]          = obj.map(x => AbstractMap(x.toMap))

  private object DurationDecoder {
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
  private def validDuration[_: P] =
    "days" | "day" | "d" |
      "hours" | "hour" | "h" |
      "minutes" | "minute" | "min" |
      "seconds" | "second" | "secs" | "sec" | "s" |
      "milliseconds" | "millisecond" | "millis" | "milli" | "ms" |
      "microseconds" | "microsecond" | "micros" | "µs" |
      "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns" |
      "Inf" | "MinusInf"
  private def duration[_: P]: P[Duration] = P((number.! ~ spaces).? ~ validDuration.!).map {
    case (value, unit) =>
      DurationDecoder(value.map(_.toDouble), unit)
  }
  private def abstractDuration[_: P]: P[AbstractDuration] = duration.map(AbstractDuration)

  private def createRange(start: Int, method: String, end: Int, optStep: Option[Int]): Range = {
    val baseRange = method match {
      case "to"    => start to end
      case "until" => start until end
    }

    optStep.map(step => baseRange by step).getOrElse(baseRange)
  }
  private def intRange[_: P]: P[Range] =
    P(
      integral.! ~ spaces ~ ("to" | "until").! ~ spaces ~ integral.! ~ (spaces ~ "by" ~ spaces ~ integral.!).? ~ optionalSpaces
    ).map {
      case (start, method, end, optStep) => createRange(start.toInt, method, end.toInt, optStep.map(_.toInt))
    }
  private def charRange[_: P]: P[Range] =
    P(
      "'" ~ AnyChar.! ~ "'" ~ spaces ~ ("to" | "until").! ~ spaces ~ "'" ~ AnyChar.! ~ "'" ~ (spaces ~ "by" ~ spaces ~ integral.!).? ~ optionalSpaces
    ).map {
      case (start, method, end, optStep) => createRange(start.head.toInt, method, end.head.toInt, optStep.map(_.toInt))
    }
  private def range[_: P]: P[Range]                 = intRange | charRange
  private def abstractRange[_: P]: P[AbstractRange] = range.map(AbstractRange)

  private def jsonExpr[_: P]: P[AbstractValue] =
    singleLineComment.rep ~ P(
      optionalSpaces ~ (abstractMap | abstractList | abstractDuration | abstractRange |
        abstractString | abstractBool | abstractNone | abstractNumber) ~ optionalSpaces
    ) ~ singleLineComment.rep

  def parse(confStr: String): Result[Config] = {
    fastparse.parse(confStr, jsonExpr(_)) match {
      case Parsed.Success(result, _) =>
        result match {
          case map: AbstractMap => Right(Config(map))
          case _ =>
            Left(new ConfigSyntaxError("The whole configuration must be represented as a pseudo-json object"))
        }
      case x: Parsed.Failure => Left(new ConfigSyntaxError(x.msg))
    }
  }
}
