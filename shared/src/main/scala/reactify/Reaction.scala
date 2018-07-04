package reactify

trait Reaction[T] extends Ordered[Reaction[T]] {
  def apply(value: T): Unit
  def priority: Double = Priority.Normal

  override def compare(that: Reaction[T]): Int = this.priority.compare(that.priority)
}

object Reaction {
  def apply[T](f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = FunctionReaction[T](f, priority)
}