package com.lambdista
package config
package exception

/**
  * Exception representing this fact: a given key cannot be found in the configuration.
  *
  * @param name the name of the key
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
class KeyNotFoundException(name: String) extends RuntimeException(s"No such key: $name")
