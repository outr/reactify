package reactify

trait Val[T] extends Reactive[T] {
  def state: State[T]

  def get: T = state.value
  def apply(): T = get
}

object Val {
  def apply[T](value: => T): Val[T] = new StandardVal[T](value)
}