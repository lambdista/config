package com.lambdista.config

/**
  * The base exception type for config errors.
  *
  * @author Alessandro Lacava (@lambdista) 
  * @since 2019-01-18
  */
sealed abstract class Error extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

/**
  * This class signals a syntax error in configuration.
  *
  * @param parseError the parse error
  *
  * @author Alessandro Lacava
  * @since 2019-01-18
  */
final class ConfigSyntaxError(parseError: String) extends Error {
  override def getMessage: String = parseError
}

/**
  * This error signals that a conversion from `abstractValue` to a [[ConcreteValue]] cannot be done.
  *
  * @param abstractValue the [[AbstractValue]] that was tried to convert
  * @author Alessandro Lacava
  * @since 2019-01-18
  */
final class ConversionError(abstractValue: AbstractValue) extends Error {
  override def getMessage: String = s"Could not convert ${abstractValue.describe} to the type requested"
}

/**
  * This error signals that a given key cannot be found in the configuration.
  *
  * @param name the name of the key
  *
  * @author Alessandro Lacava
  * @since 2019-01-18
  */
final class KeyNotFoundError(name: String) extends Error {
  override def getMessage: String = s"No such key: $name"
}