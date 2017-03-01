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

  override def changes(listener: ChangeListener[T]): (T) => Unit = {
    attach(ChangeListener.createFunction(listener, Some(get)))
  }
}