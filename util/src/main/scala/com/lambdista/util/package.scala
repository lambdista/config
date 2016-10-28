package com.lambdista

import scala.collection.generic.CanBuildFrom

/**
  * Utility methods.
  *
  * @author Alessandro Lacava
  * @since 2015-11-27
  */
package object util {
  def traverse[A, B, M[X] <: TraversableOnce[X]](
    ms: M[Option[A]]
  )(f: A => B)(implicit cbf: CanBuildFrom[M[A], B, M[B]]): Option[M[B]] = {
    val builder = cbf()
    builder.sizeHint(ms.size)

    if (ms.exists(_.isEmpty)) None
    else {
      ms foreach (x => builder += f(x.get))
      Some(builder.result)
    }
  }

  def sequence[A, M[X] <: TraversableOnce[X]](ms: M[Option[A]])(
    implicit cbf: CanBuildFrom[M[A], A, M[A]]
  ): Option[M[A]] = traverse(ms)(identity)
}
