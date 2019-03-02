package reactify.standard

import java.util.concurrent.atomic.AtomicLong

import reactify.transaction.Transaction
import reactify.{State, Var}

class StandardVar[T](f: => T, override val mode: Var.Mode, override val name: Option[String]) extends Var[T] {
  private lazy val counter = new AtomicLong(0L)

  private var _state: State[T] = new State[T](this, counter.incrementAndGet(), () => f)

  _state.update()

  override def state: State[T] = _state

  override def set(value: => T): Unit = synchronized {
    val previous = _state
    mode match {
      case Var.Mode.Normal => {
        _state = new State[T](this, counter.incrementAndGet(), () => value)
        _state.update(Some(previous))
        Transaction.change(this, previous.function, _state.function)
      }
      case Var.Mode.Static => {
        val staticValue: T = value
        _state = new State[T](this, counter.incrementAndGet(), () => staticValue)
        _state.update(None)
        Transaction.change(this, previous.function, _state.function)
      }
    }
  }
}