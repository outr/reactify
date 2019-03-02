package reactify.reaction

import reactify.Priority

/**
  * Reaction may be thought of similar to `Observer` or `Listener` in other libraries. A Reaction may be added to a
  * Reactive in order react when a value is fired upon it.
  *
  * @tparam T the type received by this Reaction
  */
trait Reaction[T] extends Ordered[Reaction[T]] {
  /**
    * Invoked when a new value is received by the associated Reactive
    *
    * @param value the new value
    * @param previous the previous value, if one was defined
    * @return
    */
  def apply(value: T, previous: Option[T]): ReactionStatus

  /**
    * Priority of this Reaction in contrast to other Reactions attached to the same Reactive. A higher value represents
    * a higher position in reactions list. See Priority for pre-defined values.
    */
  def priority: Double = Priority.Normal

  override def compare(that: Reaction[T]): Int = this.priority.compare(that.priority)
}

object Reaction {
  def apply[T](f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = FunctionReaction[T](f, priority)

  def changes[T](f: (T, T) => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    ChangeFunctionReaction[T](f, priority)
  }
}