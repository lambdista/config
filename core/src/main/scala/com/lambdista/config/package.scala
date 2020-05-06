package com.lambdista

/**
  *
  *
  * @author Alessandro Lacava
  * @since 2020-05-06
  */
package object config {
  type Result[A] = Either[Error, A]
  object Result {
    def attempt[A](a: => A): Result[A] = {
      try Right(a)
      catch {
        case e: Error => Left(e)
      }
    }
  }
}
