package reactify

import reactify.instance.StateInstanceManager

trait State[T] extends Observable[T] {
  def distinct: Boolean
  def observing: Set[Observable[_]]
  final def get: T = try {
    value()
  } finally {
    StateInstanceManager.referenced(this)
  }
  def apply(): T = get

  protected def value(): T

  protected def set(value: => T): Unit
  protected def static(value: T): Unit = set(value)

  def attachAndFire(f: T => Unit): Listener[T] = {
    val listener = attach(f)
    fire(get, InvocationType.Direct)
    listener
  }

  override def changes(listener: ChangeListener[T]): Listener[T] = {
    attach(ChangeListener.createFunction(listener, Some(get)))
  }

  protected[reactify] def changed(value: T, previous: T, `type`: InvocationType): Unit = if (!distinct || value != previous) {
    fire(value, `type`)
  }
}