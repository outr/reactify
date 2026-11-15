package test

import reactify.Var

import scala.util.Try

/** A Var with no state yet, as a subclass may make one. */
class UnsetProbe[T] extends Var[T]() {
  def stateNow: Try[T] = state
}
