package reactify

class Var[T] private(function: () => T) extends State[T](function) {
  override def set(value: => T): Unit = super.set(value)
}

object Var {
  def apply[T](value: => T): Var[T] = new Var[T](() => value)
}