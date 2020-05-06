package com.lambdista

import scala.util.{Either, Right}

/**
  *
  *
  * @author Alessandro Lacava
  * @since 2020-05-06
  */
package object config {
  type Result[A] = Either[Error, A]
  object Result {
    def orElse[A, B, A1 >: A, B1 >: B](main: Either[A, B], or: => Either[A1, B1]): Either[A1, B1] = main match {
      case Right(_) => main
      case _        => or
    }

    def attempt[A](a: => A): Result[A] = {
      try Right(a)
      catch {
        case e: Error => Left(e)
      }
    }
  }
}
