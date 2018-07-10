package reactify

import reactify.group.ValGroup
import reactify.standard.StandardVal

trait Val[T] extends Reactive[T] {
  def state: State[T]

  def get: T = state.value
  def apply(): T = get

  def and(that: Val[T]): Val[T] = ValGroup[T](None, List(this, that))

  def map[R](f: T => R): Val[R] = Val[R](f(get))

  override def toString: String = name.getOrElse("Val")
}

object Val {
  def apply[T](value: => T, name: Option[String] = None): Val[T] = new StandardVal[T](value, name)
}