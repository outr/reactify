package reactify

trait State[T] extends Observable[T] {
  def observing: Set[Observable[_]]
  def get: T
  def apply(): T = get
  def value: T = get

  def attachAndFire(f: T => Unit): T => Unit = {
    attach(f)
    fire(get)
    f
  }

  override def changes(listener: ChangeListener[T]): Listener[T] = {
    attach(ChangeListener.createFunction(listener, Some(get)))
  }
}

object State {
  /**
    * Creates a State instance with a defined value.
    *
    * @param value the value to set to the state.
    * @tparam T the type
    * @return a pre-defined, immutable State instance
    */
  def apply[T](value: T): State[T] = Prop[T](value)
}