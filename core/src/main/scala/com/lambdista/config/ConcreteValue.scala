package com.lambdista
package config

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.annotation.implicitNotFound

import shapeless.labelled.{FieldType, field}
import shapeless.{:: => :*:, _}

import com.lambdista.util._

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

  implicit val abstractValue: ConcreteValue[AbstractValue] = Option(_)

  implicit val abstractMap: ConcreteValue[AbstractMap] = {
    case x: AbstractMap => Some(x)
    case _              => None
  }

  implicit val boolValue: ConcreteValue[Boolean] = {
    case AbstractBool(b) => Some(b)
    case _               => None
  }

  implicit val intValue: ConcreteValue[Int] = {
    case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toInt)
    case _ => None
  }

  implicit val longValue: ConcreteValue[Long] = {
    case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toLong)
    case _ => None
  }

  implicit val doubleValue: ConcreteValue[Double] = {
    case AbstractNumber(n) => Some(n.toDouble)
    case _                 => None
  }

  implicit val charValue: ConcreteValue[Char] = {
    case AbstractChar(c) => Some(c)
    case AbstractNumber(n) if n % 1 == 0.0 => Some(n.toInt.toChar)
    case _ => None
  }

  implicit val stringValue: ConcreteValue[String] = {
    case AbstractString(s) => Some(s)
    case _                 => None
  }

  implicit val durationValue: ConcreteValue[Duration] = {
    case AbstractDuration(d) => Some(d)
    case _                   => None
  }

  implicit val rangeValue: ConcreteValue[Range] = {
    case AbstractRange(r) => Some(r)
    case _                => None
  }

  implicit def optionValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Option[A]] = {
    case x: AbstractNone.type => Some(None)
    case x                    => Some(A.apply(x))
  }

  implicit def listValue[A](implicit A: ConcreteValue[A]): ConcreteValue[List[A]] = {
    case AbstractList(xs) => sequence(xs.map(A.apply))
    case AbstractRange(xs) =>
      sequence(xs.toList.map(x => AbstractNumber(x.toDouble)).map(A.apply))
    case _ => None
  }

  implicit def vectorValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Vector[A]] =
    listValue[A].apply(_).map(_.toVector)

  implicit def setValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Set[A]] =
    listValue[A].apply(_).map(_.toSet)

  implicit def mapValue[A](implicit A: ConcreteValue[A]): ConcreteValue[Map[String, A]] = {
    def traverseMap(m: Map[String, Option[A]]): Option[Map[String, A]] = {
      @tailrec
      def go(xs: List[(String, Option[A])], acc: Map[String, A]): Option[Map[String, A]] = xs match {
        case a :: as =>
          a._2 match {
            case Some(v) => go(as, acc + ((a._1, v)))
            case None    => None
          }
        case Nil => Some(acc)
      }

      go(m.toList, Map.empty[String, A])
    }

    _.as[AbstractMap].toOption.flatMap { cm =>
      traverseMap(cm.value.map { case (key, value) => key -> A.apply(value) })
    }
  }

  implicit def genericValue[A, R <: HList](
    implicit gen: LabelledGeneric.Aux[A, R],
    fromMap: Lazy[FromMap[R]]
  ): ConcreteValue[A] = _.as[AbstractMap].toOption.flatMap(x => fromMap.value(x.value).map(gen.from))

  trait FromMap[L <: HList] {
    def apply(m: Map[String, AbstractValue]): Option[L]
  }

  trait LowPriorityFromMap extends LowPriorityFromMap0 {
    implicit def hconsFromMap12[K <: Symbol, V, T <: HList](
      implicit witness: Witness.Aux[K],
      concreteValue: ConcreteValue[V],
      fromMapT: Lazy[FromMap[T]],
      ev: V <:< Option[_]
    ): FromMap[FieldType[K, V] :*: T] = hconsFromMap1(Some(AbstractNone))
  }

  trait LowPriorityFromMap0 {
    implicit def hconsFromMap11[K <: Symbol, V, T <: HList](
      implicit witness: Witness.Aux[K],
      concreteValue: ConcreteValue[V],
      fromMapT: Lazy[FromMap[T]]
    ): FromMap[FieldType[K, V] :*: T] = hconsFromMap1(None)
  }

  private def hconsFromMap1[K <: Symbol, V, T <: HList](default: => Option[AbstractValue])(
    implicit witness: Witness.Aux[K],
    concreteValue: ConcreteValue[V],
    fromMapT: Lazy[FromMap[T]]
  ): FromMap[FieldType[K, V] :*: T] =
    m =>
      for {
        v <- m.get(witness.value.name) orElse default
        h <- concreteValue.apply(v)
        t <- fromMapT.value(m)
      } yield field[K](h) :: t

  object FromMap extends LowPriorityFromMap {
    implicit val hnilFromMap: FromMap[HNil] = new FromMap[HNil] {
      def apply(m: Map[String, AbstractValue]): Option[HNil] = Some(HNil)
    }

    implicit def hconsFromMap01[K <: Symbol, V, R <: HList, T <: HList](
      implicit witness: Witness.Aux[K],
      gen: LabelledGeneric.Aux[V, R],
      fromMapH: Lazy[FromMap[R]],
      fromMapT: FromMap[T]
    ): FromMap[FieldType[K, V] :*: T] = hconsFromMap0(None)

    implicit def hconsFromMap02[K <: Symbol, V, R <: HList, T <: HList](
      implicit witness: Witness.Aux[K],
      gen: LabelledGeneric.Aux[V, R],
      fromMapH: Lazy[FromMap[R]],
      fromMapT: FromMap[T],
      ev: V <:< Option[_]
    ): FromMap[FieldType[K, V] :*: T] = hconsFromMap0(Some(AbstractNone))

    private def hconsFromMap0[K <: Symbol, V, R <: HList, T <: HList](default: => Option[AbstractValue])(
      implicit witness: Witness.Aux[K],
      gen: LabelledGeneric.Aux[V, R],
      fromMapH: Lazy[FromMap[R]],
      fromMapT: FromMap[T]
    ): FromMap[FieldType[K, V] :*: T] =
      m =>
        for {
          v <- m.get(witness.value.name) orElse default
          r <- v.as[Map[String, AbstractValue]].toOption
          h <- fromMapH.value(r)
          t <- fromMapT(m)
        } yield field[K](gen.from(h)) :: t
  }
}
