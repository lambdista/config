package com.lambdista

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Abstract base class for unit testing.
  *
  * @author Alessandro Lacava
  * @since 2016-01-12
  */
abstract class UnitSpec extends AnyFlatSpec with Matchers
