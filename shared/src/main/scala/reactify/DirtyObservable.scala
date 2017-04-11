package reactify

import java.util.concurrent.atomic.AtomicReference

/**
  * DirtyObservable provides a mix-in to avoid extraneous calling of listeners. Firing of values set an internal `dirty`
  * value (replacing the previous value if set) and only invokes the listeners upon a call to `update()` which will then
  * reset the dirty status. This offers the benefit of tight-loop invocations calling the listeners fewer times as well
  * as the capacity to control the thread which invokes the listeners.
  *
  * @tparam T the type of value this Observable will receive
  */
trait DirtyObservable[T] extends Observable[T] {
  private lazy val dirty = new AtomicReference[Option[T]](None)

  /**
    * True if a value has been fired against this Observable since the last call to update.
    */
  def isDirty: Boolean = dirty.get().nonEmpty

  override protected[reactify] def fire(value: T): Unit = dirty.set(Some(value))

  /**
    * Checks if a value has been fired since the last call to update and will invoke listeners with only the last value
    * fired and then resetting the dirty state.
    */
  def update(): Unit = dirty.getAndSet(None).foreach { value =>
    fireRecursive(value, Invocation().reset(), observers)
  }
}