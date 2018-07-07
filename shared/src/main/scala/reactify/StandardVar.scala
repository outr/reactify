package reactify

import java.util.concurrent.atomic.AtomicLong

class StandardVar[T](f: => T, val name: Option[String]) extends Var[T] {
  private lazy val counter = new AtomicLong(0L)

  private var _state: State[T] = new State[T](this, counter.incrementAndGet(), () => f)

  _state.update(None)

  override def state: State[T] = _state

  override def set(value: => T): Unit = synchronized {
    val previous = _state
    _state = new State[T](this, counter.incrementAndGet(), () => value)
    _state.update(Some(previous))
  }
}