package test

import reactify.Var

import scala.util.Try

/** A Var whose protected state, previous value and function a spec can read and write, as a subclass may. */
class StateProbe[T](first: T) extends Var[T]() {
  function = () => first
  state = scala.util.Success(first)

  def stateNow: Try[T] = state
  def stateTo(t: Try[T]): Unit = state = t
  def previousNow: Option[T] = previous
  def previousTo(p: Option[T]): Unit = previous = p
  def functionNow: () => T = function
}
