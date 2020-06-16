package com.lambdista
package config

import scala.annotation.{implicitNotFound, tailrec}
import scala.concurrent.duration.Duration

import com.lambdista.util._
import magnolia._

/**
  * Type class used to convert an [[AbstractValue]] into a concrete Scala value.
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
@implicitNotFound("No instance, of the ConcreteValue type class, found for ${A}")
trait ConcreteValue[A] {
  def apply(abstractValue: AbstractValue): Option[A]
}

object ConcreteValue {
  def apply[A: ConcreteValue]: ConcreteValue[A] = implicitly[ConcreteValue[A]]

  implicit val abstractValue: ConcreteValue[AbstractValue] =
    new ConcreteValue[AbstractValue] {
      override def apply(abstractValue: AbstractValue): Option[AbstractValue] =
        Some(abstractValue)
    }

  implicit val abstractMap: ConcreteValue[AbstractMap] =
    new ConcreteValue[AbstractMap] {
      override def apply(abstractValue: AbstractValue): Option[AbstractMap] =
        abstractValue match {
          case x: AbstractMap => Some(x)
          case _              => None
        }
    }

  implicit val boolValue: ConcreteValue[Boolean] = new ConcreteValue[Boolean] {
    override def apply(abstractValue: AbstractValue): Option[Boolean] =
      abstractValue match {
        case AbstractBool(b) => Some(b)
        case _               => None
      }
  }

  implicit val intValue: ConcreteValue[Int] = new ConcreteValue[Int] {
    override def apply(abstractValue: AbstractValue): Option[Int] =
      abstractValue match {
        case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toInt)
        case _ => None
      }
  }

  implicit val longValue: ConcreteValue[Long] = new ConcreteValue[Long] {
    override def apply(abstractValue: AbstractValue): Option[Long] =
      abstractValue match {
        case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toLong)
        case _ => None
      }
  }

  implicit val doubleValue: ConcreteValue[Double] = new ConcreteValue[Double] {
    override def apply(abstractValue: AbstractValue): Option[Double] =
      abstractValue match {
        case AbstractNumber(n) => Some(n.toDouble)
        case _                 => None
      }
  }

  implicit val charValue: ConcreteValue[Char] = new ConcreteValue[Char] {
    override def apply(abstractValue: AbstractValue): Option[Char] =
      abstractValue match {
        case AbstractChar(c) => Some(c)
        case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toInt.toChar)
        case _ => None
      }
  }

  implicit val stringValue: ConcreteValue[String] = new ConcreteValue[String] {
    override def apply(abstractValue: AbstractValue): Option[String] =
      abstractValue match {
        case AbstractString(s) => Some(s)
        case _                 => None
      }
  }

  implicit val durationValue: ConcreteValue[Duration] =
    new ConcreteValue[Duration] {
      def apply(abstractValue: AbstractValue): Option[Duration] =
        abstractValue match {
          case AbstractDuration(d) => Some(d)
          case _                   => None
        }
    }

  implicit val rangeValue: ConcreteValue[Range] = new ConcreteValue[Range] {
    def apply(abstractValue: AbstractValue): Option[Range] =
      abstractValue match {
        case AbstractRange(r) => Some(r)
        case _                => None
      }
  }

  implicit def optionValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Option[A]] =
    new ConcreteValue[Option[A]] {
      override def apply(abstractValue: AbstractValue): Option[Option[A]] =
        abstractValue match {
          case _: AbstractNone.type => Some(None)
          case x                    => Some(A.apply(x))
        }
    }

  implicit def listValue[A](implicit A: ConcreteValue[A]): ConcreteValue[List[A]] =
    new ConcreteValue[List[A]] {
      def apply(abstractValue: AbstractValue): Option[List[A]] =
        abstractValue match {
          case AbstractList(xs) => sequence(xs.map(A.apply))
          case AbstractRange(xs) =>
            sequence(xs.toList.map(x => AbstractNumber(x.toDouble)).map(A.apply))
          case _ => None
        }
    }

  implicit def vectorValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Vector[A]] =
    new ConcreteValue[Vector[A]] {
      def apply(abstractValue: AbstractValue): Option[Vector[A]] =
        listValue[A].apply(abstractValue).map(_.toVector)
    }

  implicit def setValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Set[A]] =
    new ConcreteValue[Set[A]] {
      def apply(abstractValue: AbstractValue): Option[Set[A]] =
        listValue[A].apply(abstractValue).map(_.toSet)
    }

  implicit def mapValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Map[String, A]] = {
    def traverseMap(m: Map[String, Option[A]]): Option[Map[String, A]] = {
      @tailrec
      def go(xs: List[(String, Option[A])], acc: Map[String, A]): Option[Map[String, A]] =
        xs match {
          case a :: as =>
            a._2 match {
              case Some(v) => go(as, acc + ((a._1, v)))
              case None    => None
            }
          case Nil => Some(acc)
        }

      go(m.toList, Map.empty[String, A])
    }

    new ConcreteValue[Map[String, A]] {
      def apply(v: AbstractValue): Option[Map[String, A]] =
        v.as[AbstractMap].toOption.flatMap { cm =>
          traverseMap(cm.value.map { case (key, value) => key -> A.apply(value) })
        }
    }
  }

  type Typeclass[A] = ConcreteValue[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): ConcreteValue[A] =
    new ConcreteValue[A] {
      override def apply(abstractValue: AbstractValue): Option[A] = {
        def decodeMap(m: Map[String, AbstractValue]): Option[A] = {
          val res: Either[List[Throwable], A] = ctx.constructEither { p =>
            val v: AbstractValue = m.getOrElse(p.label, AbstractNone)
            p.typeclass.apply(v).map(Right(_)).getOrElse(Left(new ConversionError(v)))
          }
          res.toOption
        }

        abstractValue match {
          case AbstractMap(m) => decodeMap(m)
          case _              => None
        }
      }
    }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): ConcreteValue[A] =
    new ConcreteValue[A] {
      override def apply(abstractValue: AbstractValue): Option[A] = {
        def applyToSub(s: Subtype[Typeclass, A]): Option[A] = s.typeclass.apply(abstractValue)

        ctx.subtypes.collectFirst {
          case x if applyToSub(x).isDefined => applyToSub(x).get
        }
      }
    }

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]
}
