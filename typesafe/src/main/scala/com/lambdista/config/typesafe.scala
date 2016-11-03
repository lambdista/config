package com.lambdista
package config

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

import com.typesafe.config.{ConfigList, ConfigObject, ConfigValue, ConfigValueType, Config => TSConfig}

import com.lambdista.config.exception.TypesafeConversionException
import com.lambdista.util.sequence
import com.lambdista.util.syntax.std.option._

import java.lang.{Boolean => JBoolean}

/**
  * Adapter apt to load Typesafe Config.
  *
  * @author Alessandro Lacava
  * @since 2016-01-07
  */
object typesafe {
  implicit val tsConfigLoader = new ConfigLoader[TSConfig] {
    override def load(resource: TSConfig): Try[Config] = {
      def convertTypesafeConfig(tsConfig: TSConfig): Try[AbstractMap] = {
        def unwrap[T: ClassTag](tsConfigValue: ConfigValue): Try[T] = {
          tsConfigValue.unwrapped match {
            case t: T => Success(t)
            case _ =>
              val className = implicitly[ClassTag[T]].runtimeClass.getName
              Failure(
                new TypesafeConversionException(s"Could not convert $tsConfigValue to underlying type $className")
              )
          }
        }

        def convertConfigObject(tsConfigValue: ConfigValue): Try[AbstractMap] =
          convertTypesafeConfig(tsConfigValue.asInstanceOf[ConfigObject].toConfig)

        def convertConfigList(tsConfigList: ConfigList): Try[AbstractList] = {
          val list = tsConfigList.asScala.toList.map(v => convertConfigValue(v).toOption)

          sequence(list)
            .map(AbstractList)
            .toTry(new TypesafeConversionException(s"Could not convert $tsConfigList to a ConfigValue"))
        }

        def convertConfigValue(tsConfigValue: ConfigValue): Try[AbstractValue] = {
          tsConfigValue.valueType match {
            case ConfigValueType.NULL    => Success(AbstractNone)
            case ConfigValueType.BOOLEAN => unwrap[JBoolean](tsConfigValue).map(x => AbstractBool(x.booleanValue()))
            case ConfigValueType.NUMBER  => unwrap[Number](tsConfigValue).map(n => AbstractNumber(n.doubleValue))
            case ConfigValueType.STRING  => unwrap[String](tsConfigValue).map(AbstractString)
            case ConfigValueType.OBJECT  => convertConfigObject(tsConfigValue)
            case ConfigValueType.LIST    => convertConfigList(tsConfigValue.asInstanceOf[ConfigList])

            case _ => Failure(new TypesafeConversionException(s"Could not convert $tsConfigValue to a ConfigValue"))
          }
        }

        def tsConfigEntriesAsAbstractMap(tsConfigEntries: List[(String, ConfigValue)]): Try[AbstractMap] = {
          @tailrec
          def go(acc: Map[String, AbstractValue], tsConfigEntries: List[(String, ConfigValue)]): Try[AbstractMap] = {
            tsConfigEntries match {
              case Nil => Success(AbstractMap(acc))
              case (k, v) :: es =>
                convertConfigValue(v) match {
                  case Failure(err) => Failure(err)
                  case Success(cv)  => go(acc + (k -> cv), es)
                }
            }
          }

          go(Map.empty[String, AbstractValue], tsConfigEntries)
        }

        val tsConfigEntries: List[(String, ConfigValue)] = tsConfig.entrySet.asScala.toList.map { entry =>
          val key   = entry.getKey
          val value = entry.getValue

          if (key.indexOf('.') == -1) {
            key -> value
          } else {
            val rootKey = key.takeWhile(_ != '.')
            rootKey -> tsConfig.getObject(rootKey)
          }
        }

        tsConfigEntriesAsAbstractMap(tsConfigEntries)
      }

      convertTypesafeConfig(resource).map(cm => Config(cm))
    }
  }
}
