package reactify

case class ChangeFunctionReaction[T](f: (T, T) => Unit, override val priority: Double) extends Reaction[T] {
  private var previous: Option[T] = None

  override def apply(value: T, previous: Option[T]): Unit = try {
    previous.orElse(this.previous).foreach { p =>
      f(p, value)
    }
  } finally {
    this.previous = Some(value)
  }
}
