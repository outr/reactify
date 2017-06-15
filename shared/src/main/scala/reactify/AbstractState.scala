package reactify

import reactify.instance.{RecursionMode, StateInstanceManager}

class AbstractState[T](override val distinct: Boolean,
                       cache: Boolean,
                       recursion: RecursionMode) extends State[T] {
  private val manager = new StateInstanceManager[T](this, cache, recursion)

  override def observing: Set[Observable[_]] = manager.observables

  override protected def value(): T = manager.get

  override def set(value: => T): Unit = manager.replaceInstance(() => value)
}