package com.lambdista

import scala.collection.BuildFrom

/**
  * Utility methods.
  *
  * @author Alessandro Lacava
  * @author Giuseppe Cannella
  * @since 2019-06-02
  */
package object util {

  def traverse1[A, B, M[X] <: TraversableOnce[X]](
    ms: M[Option[A]]
  )(f: A => B)(implicit cbf: BuildFrom[M[Option[A]], B, M[B]]): Option[M[B]] = {

    val builder = cbf.newBuilder(ms)
    builder.sizeHint(ms.size)

    if (ms.exists(_.isEmpty)) None
    else {
      ms foreach { x =>
        val t = f(x.get)
        builder += t
      }
      val x = builder.result
      Some(x)
    }
  }

  def sequence[A, M[X] <: TraversableOnce[X]](ms: M[Option[A]])(
    implicit cbf: BuildFrom[M[Option[A]], A, M[A]]
  ): Option[M[A]] = traverse1(ms)(identity)
}
