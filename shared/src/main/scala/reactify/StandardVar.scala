package reactify

class StandardVar[T](f: => T) extends Var[T] {
  private var _state: State[T] = new State[T](this, () => f)

  override def state: State[T] = _state

  override def set(value: => T): Unit = synchronized {
    _state.clearReferences()
    _state = new State[T](this, () => value)
  }
}
