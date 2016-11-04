package com.lambdista
package config

import java.io.{File, InputStream}
import java.net.{URI, URL}
import java.nio.file.Path

import scala.annotation.implicitNotFound
import scala.io.Source
import scala.util.Try

/**
  * This type class represents a configuration loader.
  *
  * @author Alessandro Lacava
  * @since 2015-12-01
  */
@implicitNotFound("No instance, of the ConfigLoader type class, found for ${R}")
trait ConfigLoader[R] {

  /**
    * Loads the configuration from `resource`.
    *
    * @param resource the resource representing the configuration
    * @return a `Try[Config]`. If it's a `Failure` it means that either there has been a problem loading the resource
    *         or the configuration syntax is not correct.
    */
  def load(resource: R): Try[Config]
}

object ConfigLoader {
  def apply[R: ConfigLoader]: ConfigLoader[R] = implicitly[ConfigLoader[R]]

  implicit val stringLoader: ConfigLoader[String] = ConfigParser.parse

  implicit val sourceLoader: ConfigLoader[Source] = resource => {
    val lines = try resource.mkString
    finally resource.close()

    ConfigLoader[String].load(lines)
  }

  implicit val fileLoader: ConfigLoader[File] = resource =>
    Try(Source.fromFile(resource)).flatMap(ConfigLoader[Source].load)

  implicit val pathLoader: ConfigLoader[Path] = resource =>
    Try(Source.fromFile(resource.toFile)).flatMap(ConfigLoader[Source].load)

  implicit val inputStreamLoader: ConfigLoader[InputStream] = resource =>
    Try(Source.fromInputStream(resource)).flatMap(ConfigLoader[Source].load)

  implicit val uriLoader: ConfigLoader[URI] = resource =>
    Try(Source.fromFile(resource)).flatMap(ConfigLoader[Source].load)

  implicit val urlLoader: ConfigLoader[URL] = resource =>
    Try(Source.fromURL(resource)).flatMap(ConfigLoader[Source].load)
}
