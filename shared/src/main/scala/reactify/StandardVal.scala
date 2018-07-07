package reactify

class StandardVal[T](f: => T, val name: Option[String]) extends Val[T] {
  override val state: State[T] = new State[T](this, 1, () => f)

  state.update(None)
}
