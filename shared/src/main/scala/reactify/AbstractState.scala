package reactify

import reactify.instance.{RecursionMode, StateInstanceManager}

class AbstractState[T](override val distinct: Boolean,
                       cache: Boolean,
                       recursion: RecursionMode,
                       transactional: Boolean,
                       onUpdate: Boolean) extends State[T] {
  protected val manager = new StateInstanceManager[T](this, cache, recursion, transactional, onUpdate)

  override def observing: Set[Observable[_]] = manager.observables

  override protected def value(): T = manager.get

  override protected def set(value: => T): Unit = {
    manager.replaceInstance(() => value)
  }
}