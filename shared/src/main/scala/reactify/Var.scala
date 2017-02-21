package reactify

class Var[T] private(instance: StateInstance[T]) extends State[T](instance) {
  override def set(value: => T): Unit = super.set(value)
}

object Var {
  def apply[T](value: => T): Var[T] = {
    val v = new Var[T](new StateInstance[T](() => value))
    v.set(value)
    v
  }
}