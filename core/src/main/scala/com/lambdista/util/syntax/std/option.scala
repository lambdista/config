package com.lambdista.util.syntax.std

import scala.util.{Failure, Success, Try}

/**
  * Extension class for `Option[A]`.
  *
  * @author Alessandro Lacava
  * @since 2016-01-05
  */
final class OptionOps[A](val opt: Option[A]) extends AnyVal {
  def toTry(ifFailure: => Throwable): Try[A] = opt match {
    case Some(x) => Success(x)
    case None    => Failure(ifFailure)
  }
}

trait OptionSyntax {
  implicit def optionSyntax[A](opt: Option[A]): OptionOps[A] = new OptionOps(opt)
}