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

  implicit val stringLoader = new ConfigLoader[String] {
    override def load(resource: String): Try[Config] = ConfigParser.parse(resource)
  }

  implicit val sourceLoader = new ConfigLoader[Source] {
    override def load(resource: Source): Try[Config] = {
      val lines = try resource.mkString finally resource.close()

      ConfigLoader[String].load(lines)
    }
  }

  implicit val fileLoader = new ConfigLoader[File] {
    override def load(resource: File): Try[Config] = ConfigLoader[Source].load(Source.fromFile(resource))
  }

  implicit val pathLoader = new ConfigLoader[Path] {
    override def load(resource: Path): Try[Config] = ConfigLoader[Source].load(Source.fromFile(resource.toFile))
  }

  implicit val inputStreamLoader = new ConfigLoader[InputStream] {
    override def load(resource: InputStream): Try[Config] = ConfigLoader[Source].load(Source.fromInputStream(resource))
  }

  implicit val uriLoader = new ConfigLoader[URI] {
    override def load(resource: URI): Try[Config] = ConfigLoader[Source].load(Source.fromFile(resource))
  }

  implicit val urlLoader = new ConfigLoader[URL] {
    override def load(resource: URL): Try[Config] = ConfigLoader[Source].load(Source.fromURL(resource))
  }
}