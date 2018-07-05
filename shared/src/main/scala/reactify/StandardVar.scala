package reactify

class StandardVar[T](f: => T) extends Var[T] {
  private var _state: State[T] = new State[T](this, () => f)

  _state.update(None)

  override def state: State[T] = _state

  override def set(value: => T): Unit = synchronized {
    val previous = _state
    _state.clearReferences()
    _state = new State[T](this, () => value)
    _state.update(Some(previous))
  }
}