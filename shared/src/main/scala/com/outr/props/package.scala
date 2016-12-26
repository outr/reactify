package com.outr

import scala.language.implicitConversions

package object props {
  /**
    * Converts a `State[T]` to `T` implicitly. This is useful for DSL type-based operations like `5 + stateVar`.
    */
  implicit def state2Value[T](p: State[T]): T = p.get

  implicit val intConnector: DepConnector[Int, Int] = new DepConnector[Int, Int] {
    override def combine(variable: => Int, adjustment: => Int): Int = variable + adjustment

    override def extract(value: => Int, adjustment: => Int): Int = value - adjustment
  }

  implicit val doubleConnector: DepConnector[Double, Double] = new DepConnector[Double, Double] {
    override def combine(variable: => Double, adjustment: => Double): Double = variable + adjustment

    override def extract(value: => Double, adjustment: => Double): Double = value - adjustment
  }

  /**
    * Syntactic sugar for mutating collections in a `StateChannel`
    */
  implicit class SeqStateChannel[T](v: StateChannel[Seq[T]]) {
    def +=(t: T): Unit = {
      val updated = v() :+ t
      v := updated
    }

    def -=(t: T): Unit = {
      val updated = v().filterNot(_ == t)
      v := updated
    }

    def ++=(seq: Seq[T]): Unit = {
      val updated = v() ++ seq
      v := updated
    }

    def clear(): Unit = v := Vector.empty
  }
}