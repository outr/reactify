package reactify.reaction

case class FunctionReaction[T](f: T => Unit, override val priority: Double) extends Reaction[T] {
  override def apply(value: T, previous: Option[T]): ReactionStatus = {
    f(value)
    ReactionStatus.Continue
  }
}