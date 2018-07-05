package reactify

class StandardVal[T](f: => T) extends Val[T] {
  override val state: State[T] = new State[T](this, () => f)

  state.update(None)
}
