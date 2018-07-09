package reactify

import reactify.standard.StandardVal

trait Val[T] extends Reactive[T] {
  def state: State[T]

  def get: T = state.value
  def apply(): T = get

  override def toString: String = name.getOrElse("Val")
}

object Val {
  def apply[T](value: => T, name: Option[String] = None): Val[T] = new StandardVal[T](value, name)
}