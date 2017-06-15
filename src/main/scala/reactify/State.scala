package reactify

import reactify.instance.StateInstanceManager

trait State[T] extends Observable[T] {
  def distinct: Boolean
  def observing: Set[Observable[_]]
  final def get: T = {
    StateInstanceManager.referenced(this)
    value()
  }
  def apply(): T = get

  protected def value(): T

  def attachAndFire(f: T => Unit): Listener[T] = {
    val listener = attach(f)
    fire(get)
    listener
  }

  override def changes(listener: ChangeListener[T]): Listener[T] = {
    attach(ChangeListener.createFunction(listener, Some(get)))
  }

  protected[reactify] def changed(value: T, previous: T): Unit = if (!distinct || value != previous) {
    fire(value)
  }
}