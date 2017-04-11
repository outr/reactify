package reactify

trait Listener[T] {
  def apply(value: T): Unit
}

class FunctionListener[T](f: T => Unit) extends Listener[T] {
  override def apply(value: T): Unit = f(value)
}