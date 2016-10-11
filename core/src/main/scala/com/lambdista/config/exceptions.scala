package com.lambdista
package config

/**
  * Exception representing this fact: a given key cannot be found in the configuration.
  *
  * @param name the name of the key
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
class KeyNotFoundException(name: String) extends RuntimeException(s"No such key: $name")

/**
  * Exception representing this fact: a conversion from `abstractValue` to a [[ConcreteValue]] cannot be done.
  *
  * @param abstractValue the [[AbstractValue]] that was tried to convert
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
class ConversionException(abstractValue: AbstractValue)
    extends RuntimeException(s"Could not convert ${abstractValue.describe} to the type requested")

/**
  * Exception representing this fact: the configuration syntax is wrong.
  *
  * @param parseError the parse error
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
class ConfigSyntaxException(parseError: String) extends RuntimeException(parseError)
