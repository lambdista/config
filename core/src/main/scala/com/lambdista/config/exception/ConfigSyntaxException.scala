package com.lambdista
package config
package exception

/**
  * Exception representing this fact: the configuration syntax is wrong.
  *
  * @param parseError the parse error
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
class ConfigSyntaxException(parseError: String) extends RuntimeException(parseError)
