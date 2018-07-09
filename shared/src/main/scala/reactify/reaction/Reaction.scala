package reactify.reaction

import reactify.Priority

trait Reaction[T] extends Ordered[Reaction[T]] {
  def apply(value: T, previous: Option[T]): ReactionStatus
  def priority: Double = Priority.Normal

  override def compare(that: Reaction[T]): Int = this.priority.compare(that.priority)
}

object Reaction {
  def apply[T](f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = FunctionReaction[T](f, priority)

  def changes[T](f: (T, T) => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    ChangeFunctionReaction[T](f, priority)
  }
}