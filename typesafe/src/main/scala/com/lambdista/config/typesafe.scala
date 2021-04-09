package com.lambdista
package config

import java.lang.{Boolean => JBoolean}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

import com.lambdista.util.sequence
import com.typesafe.config.{ConfigList, ConfigObject, ConfigValue, ConfigValueType, Config => TSConfig}

/**
  * Adapter apt to load Typesafe Config.
  *
  * @author Alessandro Lacava
  * @since 2016-01-07
  */
object typesafe {
  implicit val tsConfigLoader: ConfigLoader[TSConfig] = new ConfigLoader[TSConfig] {
    override def load(resource: TSConfig): Result[Config] = {
      def convertTypesafeConfig(tsConfig: TSConfig): Result[AbstractMap] = {
        def unwrap[T: ClassTag](tsConfigValue: ConfigValue): Result[T] = {
          tsConfigValue.unwrapped match {
            case t: T => Right(t)
            case _ =>
              val className = implicitly[ClassTag[T]].runtimeClass.getName
              Left(
                new TypesafeConversionError(s"Could not convert $tsConfigValue to underlying type $className")
              )
          }
        }

        def convertConfigObject(tsConfigValue: ConfigValue): Result[AbstractMap] =
          convertTypesafeConfig(tsConfigValue.asInstanceOf[ConfigObject].toConfig)

        def convertConfigList(tsConfigList: ConfigList): Result[AbstractList] = {
          val list = tsConfigList.asScala.toList.map(v => convertConfigValue(v).toOption)

          sequence(list)
            .map(AbstractList)
            .toRight(new TypesafeConversionError(s"Could not convert $tsConfigList to a ConfigValue"))
        }

        def convertConfigValue(tsConfigValue: ConfigValue): Result[AbstractValue] = {
          tsConfigValue.valueType match {
            case ConfigValueType.NULL    => Right(AbstractNone)
            case ConfigValueType.BOOLEAN => unwrap[JBoolean](tsConfigValue).map(x => AbstractBool(x.booleanValue()))
            case ConfigValueType.NUMBER  => unwrap[Number](tsConfigValue).map(n => AbstractNumber(n.doubleValue))
            case ConfigValueType.STRING  => unwrap[String](tsConfigValue).map(AbstractString)
            case ConfigValueType.OBJECT  => convertConfigObject(tsConfigValue)
            case ConfigValueType.LIST    => convertConfigList(tsConfigValue.asInstanceOf[ConfigList])

            case _ => Left(new TypesafeConversionError(s"Could not convert $tsConfigValue to a ConfigValue"))
          }
        }

        def tsConfigEntriesAsAbstractMap(tsConfigEntries: List[(String, ConfigValue)]): Result[AbstractMap] = {
          @tailrec
          def go(acc: Map[String, AbstractValue], tsConfigEntries: List[(String, ConfigValue)]): Result[AbstractMap] = {
            tsConfigEntries match {
              case Nil => Right(AbstractMap(acc))
              case (k, v) :: es =>
                convertConfigValue(v) match {
                  case Left(err) => Left(err)
                  case Right(cv) => go(acc + (k -> cv), es)
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
