package reactify.standard

import java.util.concurrent.atomic.AtomicLong

import reactify.{Dep, State, Var}

class StandardDep[T, R](override val name: Option[String],
                        override val owner: Var[R],
                        r2TFunction: R => T,
                        t2RFunction: T => R) extends Dep[T, R] {
  private lazy val counter = new AtomicLong(0L)

  private var _state: State[T] = new State[T](this, counter.incrementAndGet(), () => r2T(owner()))

  _state.update()

  override def state: State[T] = _state

  override def set(value: => T): Unit = synchronized {
    if (!state.cached.contains(value)) {
      val previous = _state

      _state = new State[T](this, counter.incrementAndGet(), () => r2T(owner()))
      _state.update(Some(previous))

      owner := t2R(value)
    }
  }

  override def t2R(t: T): R = t2RFunction(t)

  override def r2T(r: R): T = r2TFunction(r)
}