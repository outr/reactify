package reactify

trait Val[T] extends Reactive[T] {
  def state: State[T]

  def get: T = state.value
  def apply(): T = get
}