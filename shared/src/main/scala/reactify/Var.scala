package reactify

trait Var[T] extends Val[T] with Channel[T]

object Var {
  def apply[T](value: => T): Var[T] = new StandardVar[T](value)
}