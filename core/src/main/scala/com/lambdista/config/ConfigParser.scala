package com.lambdista
package config

import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

import org.parboiled2._

/**
  * Configuration parser.
  *
  * @author Alessandro Lacava 
  * @since 2015-12-09
  */
object ConfigParser {

  private class ConfigParserImpl(val input: ParserInput) extends Parser {
    val isStringChars: Char => Boolean = c => !"\"\\".toSet(c)
    val isIdentifier: Char => Boolean = c => !":= \"".toSet(c)

    def WS = rule {
      zeroOrMore(anyOf(" \n\r\t\f"))
    }

    def cw(char: Char): Rule0 = rule {
      char ~ WS
    }

    def wcw(char: Char): Rule0 = rule {
      WS ~ char ~ WS
    }

    def digits: Rule0 = rule {
      CharPredicate.Digit.+
    }

    def exponent: Rule0 = rule {
      ignoreCase('e') ~ anyOf("+-").? ~ digits
    }

    def fractional: Rule0 = rule {
      "." ~ digits
    }

    def integer: Rule0 = rule {
      "0" | (CharPredicate.Digit19 ~ digits.?)
    }

    def number: Rule1[AbstractNumber] = rule {
      capture(anyOf("+-").? ~ integer ~ fractional.? ~ exponent.?) ~> ((x: String) => AbstractNumber.apply(x.toDouble))
    }

    def `false`: Rule1[AbstractBool] = rule {
      "false" ~ push(AbstractBool(false))
    }

    def `true`: Rule1[AbstractBool] = rule {
      "true" ~ push(AbstractBool(true))
    }

    def `null`[A]: Rule1[AbstractNone.type] = rule {
      "null" ~ push(AbstractNone)
    }

    def unicodeEscape: Rule0 = rule {
      "u" ~ 4.times(CharPredicate.HexDigit)
    }

    def singleQuote: Rule0 = rule {
      ch('"')
    }

    def escape: Rule0 = rule {
      "\\" ~ (anyOf("\"/\\bfnrt") | unicodeEscape)
    }

    def identifier: Rule1[String] = rule {
      WS ~ capture(CharPredicate.from(isIdentifier).+) ~ WS
    }

    def quotedIdentifier: Rule1[String] = rule {
      WS ~ singleQuote ~ capture(CharPredicate.from(_ != '"').+) ~ singleQuote ~ WS
    }

    def strChars: Rule0 = rule {
      CharPredicate.from(isStringChars)
    }

    def string: Rule1[AbstractString] = rule {
      WS ~ singleQuote ~ capture((strChars | escape).*) ~> AbstractString ~ singleQuote
    }

    def aChar: Rule1[Char] = rule {
      WS ~ "'" ~ capture(CharPredicate.All) ~> (_.head) ~ "'" ~ WS
    }

    def char: Rule1[AbstractChar] = rule {
      aChar ~> AbstractChar
    }

    def validDurations: Rule0 = rule {
      "days" | "day" | "d" |
      "hours" | "hour" | "h" |
      "minutes" | "minute" | "min" |
      "seconds" | "second" | "secs" | "sec" | "s" |
      "milliseconds" | "millisecond" | "millis" | "milli" | "ms" |
      "microseconds" | "microsecond" | "micros" | "µs" |
      "nanoseconds" | "nanosecond" | "nanos" | "nano" | "ns"
    }

    def duration: Rule1[AbstractDuration] = rule {
      capture(integer) ~ WS ~ capture(validDurations) ~>
        ((value: String, unit: String) => AbstractDuration(Duration.create(value.toLong, unit)))
    }

    def createRange(start: Int, method: String, end: Int, optStep: Option[Int]): Range = {
      val baseRange = method match {
        case "to" => start to end
        case "until" => start until end
      }

      optStep.map(step => baseRange by step).getOrElse(baseRange)
    }

    def intRange: Rule1[AbstractRange] = {
      rule {
        capture(integer) ~ WS ~ capture("to" | "until") ~ WS ~ capture(integer) ~ WS ~ optional(("by" ~ WS ~ capture(integer))) ~>
          ((start: String, method: String, end: String, optStep: Option[String]) => AbstractRange(createRange(start.toInt, method, end.toInt, optStep.map(_.toInt))))
      }
    }

    def charRange: Rule1[AbstractRange] = {
      rule {
        aChar ~ WS ~ capture("to" | "until") ~ WS ~ aChar ~ WS ~ optional(("by" ~ WS ~ capture(integer))) ~>
          ((start: Char, method: String, end: Char, optStep: Option[String]) => AbstractRange(createRange(start, method, end, optStep.map(_.toInt))))
      }
    }

    def range: Rule1[AbstractRange] = rule {
      intRange | charRange
    }

    def list: Rule1[AbstractList] = rule {
      cw('[') ~ configValue.*.separatedBy(wcw(',')) ~> (xs => AbstractList(xs.toList)) ~ cw(']')
    }

    def pair: Rule1[(String, AbstractValue)] = rule {
      ((identifier | quotedIdentifier) ~ (wcw(':') | wcw('=')) ~ configValue) ~> ((a: String, b: AbstractValue) => (a, b))
    }

    def map: Rule1[AbstractMap] = rule {
      wcw('{') ~ pair.*.separatedBy(wcw(',')) ~> (pairs => AbstractMap(pairs.toMap)) ~ wcw('}')
    }

    def configValue: Rule1[AbstractValue] = rule {
      WS ~ (map | list | duration | range | string | number | char | `true` | `false` | `null`) ~ WS
    }
  }

  /**
    * Parses the string representing the configuration.
    *
    * @param configStr the string representing the configuration
    * @return
    */
  def parse(configStr: String): Try[Config] = new ConfigParserImpl(configStr).map.run().map(Config.apply).recoverWith {
    case e: ParseError => Failure(new ConfigSyntaxException(e.format(configStr)))
  }
}