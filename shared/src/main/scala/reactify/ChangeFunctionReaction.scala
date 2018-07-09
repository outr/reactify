package reactify

case class ChangeFunctionReaction[T](f: (T, T) => Unit, override val priority: Double) extends Reaction[T] {
  private var previous: Option[T] = None

  override def apply(value: T, previous: Option[T]): ReactionStatus = try {
    previous.orElse(this.previous).foreach { p =>
      f(p, value)
    }
    ReactionStatus.Continue
  } finally {
    this.previous = Some(value)
  }
}
