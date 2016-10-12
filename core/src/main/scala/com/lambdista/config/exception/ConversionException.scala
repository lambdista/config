package com.lambdista
package config
package exception

/**
  * Exception representing this fact: a conversion from `abstractValue` to a [[ConcreteValue]] cannot be done.
  *
  * @param abstractValue the [[AbstractValue]] that was tried to convert
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
class ConversionException(abstractValue: AbstractValue)
    extends RuntimeException(s"Could not convert ${abstractValue.describe} to the type requested")
