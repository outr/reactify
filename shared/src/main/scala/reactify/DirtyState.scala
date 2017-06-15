package reactify

import java.util.concurrent.atomic.AtomicReference

// TODO: apply in StateInstanceManager
trait DirtyState[T] extends AbstractState[T] {
  private lazy val dirty = new AtomicReference[Option[() => T]](None)

  def isDirty: Boolean = dirty.get().nonEmpty

  def update(): Boolean = dirty.getAndSet(None) match {
    case Some(value) => {
      manager.replaceInstance(value)
      true
    }
    case None => false
  }

  override def set(value: => T): Unit = if (manager.isEmpty) {
    super.set(value)
  } else {
    dirty.set(Some(() => value))
  }
}