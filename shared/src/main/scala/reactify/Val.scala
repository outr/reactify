package reactify

import reactify.group.ValGroup
import reactify.standard.StandardVal

/**
  * Val represents a final variable that cannot be set apart from its instantiation. However, unlike a Scala `val`, a
  * `Val` may still fire changes if its value is derived from `Var`s that make it up. A `Val` is a stateful `Reactive`.
  *
  * @tparam T the type of value this Reactive receives
  */
trait Val[T] extends Reactive[T] {
  /**
    * The current State representation
    */
  def state: State[T]

  /**
    * Gets the current value from the current `State`
    */
  def get: T = state.value

  /**
    * Convenience wrapper around `get`
    */
  def apply(): T = get

  /**
    * Group multiple Vals together
    */
  def and(that: Val[T]): Val[T] = ValGroup[T](None, List(this, that))

  /**
    * Functional mapping of this Val into another Val.
    *
    * @param f conversion function
    * @tparam R the type of the new Val
    * @return Val[R]
    */
  def map[R](f: T => R): Val[R] = Val[R](f(get))

  override def toString: String = name.getOrElse("Val")
}

object Val {
  def apply[T](value: => T, name: Option[String] = None): Val[T] = new StandardVal[T](value, name)
}