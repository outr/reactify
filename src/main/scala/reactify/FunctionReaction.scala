package reactify

case class FunctionReaction[T](f: T => Unit, override val priority: Double) extends Reaction[T] {
  override def apply(value: T): Unit = f(value)
}
