package reactify

trait Var[T] extends Val[T] with Channel[T] {
  def attachAndFire(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    val reaction = attach(f, priority)
    fire(get, Some(get), reactions())
    reaction
  }
}

object Var {
  def apply[T](value: => T): Var[T] = new StandardVar[T](value)
}