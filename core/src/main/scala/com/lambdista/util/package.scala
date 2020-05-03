package com.lambdista

/**
  *
  *
  * @author Alessandro Lacava (@lambdista)
  * @since 2019-06-12
  */
package object util {
  def traverse[A, B](ms: List[Option[A]])(f: A => B): Option[List[B]] =
    if (ms.exists(_.isEmpty)) None else Some(ms.map(x => f(x.get)))

  def sequence[A](ms: List[Option[A]]): Option[List[A]] = traverse(ms)(identity)
}
