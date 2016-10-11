package com.lambdista
package config
package exception

/**
  * Exception representing a failure while trying to convert from Typesafe's config to this config's AST.
  *
  * @author Alessandro Lacava
  * @since 2016-01-07
  */
class TypesafeConversionException(message: String) extends RuntimeException(message)
