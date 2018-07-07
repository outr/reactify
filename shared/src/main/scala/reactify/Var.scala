package reactify

trait Var[T] extends Val[T] with Channel[T] {
  def attachAndFire(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    val reaction = attach(f, priority)
    fire(get, Some(get), reactions())
    reaction
  }

  override def toString: String = name.getOrElse("Var")
}

object Var {
  def apply[T](value: => T, name: Option[String] = None): Var[T] = new StandardVar[T](value, name)
}