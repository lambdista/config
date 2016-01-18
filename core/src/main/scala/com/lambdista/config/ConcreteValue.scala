package com.lambdista
package config

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.annotation.implicitNotFound

import shapeless.labelled.{FieldType, field}
import shapeless.{:: => :*:, _}

import com.lambdista.util._

/**
  * Type class used to convert an `AbstractValue` into a concrete Scala value.
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

  implicit val abstractValue: ConcreteValue[AbstractValue] = new ConcreteValue[AbstractValue] {
    override def apply(abstractValue: AbstractValue): Option[AbstractValue] = Some(abstractValue)
  }

  implicit val abstractMap: ConcreteValue[AbstractMap] = new ConcreteValue[AbstractMap] {
    override def apply(abstractValue: AbstractValue): Option[AbstractMap] = abstractValue match {
      case x: AbstractMap => Some(x)
      case _ => None
    }
  }

  implicit val boolValue: ConcreteValue[Boolean] = new ConcreteValue[Boolean] {
    override def apply(abstractValue: AbstractValue): Option[Boolean] = abstractValue match {
      case AbstractBool(b) => Some(b)
      case _ => None
    }
  }

  implicit val intValue: ConcreteValue[Int] = new ConcreteValue[Int] {
    override def apply(abstractValue: AbstractValue): Option[Int] = abstractValue match {
      case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toInt)
      case _ => None
    }
  }

  implicit val longValue: ConcreteValue[Long] = new ConcreteValue[Long] {
    override def apply(abstractValue: AbstractValue): Option[Long] = abstractValue match {
      case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toLong)
      case _ => None
    }
  }

  implicit val doubleValue: ConcreteValue[Double] = new ConcreteValue[Double] {
    override def apply(abstractValue: AbstractValue): Option[Double] = abstractValue match {
      case AbstractNumber(n) => Some(n.toDouble)
      case _ => None
    }
  }

  implicit val charValue: ConcreteValue[Char] = new ConcreteValue[Char] {
    override def apply(abstractValue: AbstractValue): Option[Char] = abstractValue match {
      case AbstractChar(c) => Some(c)
      case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toInt.toChar)
      case _ => None
    }
  }

  implicit val stringValue: ConcreteValue[String] = new ConcreteValue[String] {
    override def apply(abstractValue: AbstractValue): Option[String] = abstractValue match {
      case AbstractString(s) => Some(s)
      case _ => None
    }
  }

  implicit val durationValue: ConcreteValue[Duration] = new ConcreteValue[Duration] {
    def apply(abstractValue: AbstractValue) = abstractValue match {
      case AbstractDuration(d) => Some(d)
      case _ => None
    }
  }

  implicit val rangeValue: ConcreteValue[Range] = new ConcreteValue[Range] {
    def apply(abstractValue: AbstractValue) = abstractValue match {
      case AbstractRange(r) => Some(r)
      case _ => None
    }
  }

  implicit def optionValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Option[A]] = new ConcreteValue[Option[A]] {
    override def apply(abstractValue: AbstractValue): Option[Option[A]] = abstractValue match {
      case x: AbstractNone.type => Some(None)
      case x => Some(A.apply(x))
    }
  }

  implicit def listValue[A](implicit A: ConcreteValue[A]): ConcreteValue[List[A]] = new ConcreteValue[List[A]] {
    def apply(abstractValue: AbstractValue): Option[List[A]] = abstractValue match {
      case AbstractList(xs) => sequence(xs.map(A.apply))
      case AbstractRange(xs) => sequence(xs.toList.map(x => AbstractNumber(x.toDouble)).map(A.apply))
      case _ => None
    }
  }

  implicit def vectorValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Vector[A]] = new ConcreteValue[Vector[A]] {
    def apply(abstractValue: AbstractValue): Option[Vector[A]] = listValue[A].apply(abstractValue).map(_.toVector)
  }

  implicit def setValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Set[A]] = new ConcreteValue[Set[A]] {
    def apply(abstractValue: AbstractValue): Option[Set[A]] = listValue[A].apply(abstractValue).map(_.toSet)
  }

  implicit def mapValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Map[String, A]] = {
    def traverseMap[A](m: Map[String, Option[A]]): Option[Map[String, A]] = {
      @tailrec
      def go(xs: List[(String, Option[A])], acc: Map[String, A]): Option[Map[String, A]] = xs match {
        case a :: as => a._2 match {
          case Some(v) => go(as, acc + ((a._1, v)))
          case None => None
        }
        case Nil => Some(acc)
      }

      go(m.toList, Map.empty[String, A])
    }

    new ConcreteValue[Map[String, A]] {
      def apply(v: AbstractValue) = v.tryAs[AbstractMap].toOption.flatMap { cm =>
        traverseMap(cm.value.map { case (key, value) => key -> A.apply(value) })
      }
    }
  }

  implicit def caseClassValue[A, R <: HList](implicit
                                             gen: LabelledGeneric.Aux[A, R],
                                             fromMap: FromMap[R]
                                            ): ConcreteValue[A] = new ConcreteValue[A] {
    override def apply(abstractValue: AbstractValue): Option[A] =
      abstractValue.tryAs[AbstractMap].toOption.flatMap(x => fromMap(x.value).map(gen.from))
  }

  private trait FromMap[L <: HList] {
    def apply(m: Map[String, AbstractValue]): Option[L]
  }

  private trait LowPriorityFromMap {
    implicit def hconsFromMap1[K <: Symbol, V, T <: HList](implicit
                                                           witness: Witness.Aux[K],
                                                           concreteValue: ConcreteValue[V],
                                                           fromMapT: FromMap[T]
                                                          ): FromMap[FieldType[K, V] :*: T] =
      new FromMap[FieldType[K, V] :*: T] {
        def apply(m: Map[String, AbstractValue]): Option[FieldType[K, V] :*: T] = for {
          v <- m.get(witness.value.name)
          h <- concreteValue.apply(v)
          t <- fromMapT(m)
        } yield field[K](h) :: t
      }
  }

  private object FromMap extends LowPriorityFromMap {
    implicit val hnilFromMap: FromMap[HNil] = new FromMap[HNil] {
      def apply(m: Map[String, AbstractValue]): Option[HNil] = Some(HNil)
    }

    implicit def hconsFromMap0[K <: Symbol, V, R <: HList, T <: HList](implicit
                                                                       witness: Witness.Aux[K],
                                                                       gen: LabelledGeneric.Aux[V, R],
                                                                       fromMapH: FromMap[R],
                                                                       fromMapT: FromMap[T]
                                                                      ): FromMap[FieldType[K, V] :*: T] =
      new FromMap[FieldType[K, V] :*: T] {
        def apply(m: Map[String, AbstractValue]): Option[FieldType[K, V] :*: T] = for {
          v <- m.get(witness.value.name)
          r <- v.tryAs[Map[String, AbstractValue]].toOption
          h <- fromMapH(r)
          t <- fromMapT(m)
        } yield field[K](gen.from(h)) :: t
      }
  }
}