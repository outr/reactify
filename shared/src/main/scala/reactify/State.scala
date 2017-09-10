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

  def attachAndFire(f: T => Unit): Observer[T] = {
    val observer = attach(f)
    fire(get, InvocationType.Direct)
    observer
  }

  override def changes(observer: ChangeObserver[T]): Observer[T] = {
    attach(ChangeObserver.createFunction(observer, Some(get)))
  }

  protected[reactify] def changed(value: T, previous: T, `type`: InvocationType): Unit = if (!distinct || value != previous) {
    fire(value, `type`)
  }
}