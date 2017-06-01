package reactify

import java.util.concurrent.atomic.AtomicBoolean

trait StateChannel[T] extends State[T] with Channel[T] {
  def bind(that: StateChannel[T], setNow: BindSet = BindSet.LeftToRight): Binding[T] = {
    setNow match {
      case BindSet.LeftToRight => that := this
      case BindSet.RightToLeft => this := that
      case BindSet.None => // Nothing
    }
    val changing = new AtomicBoolean(false)
    val leftToRight = this.attach { t =>
      if (changing.compareAndSet(false, true)) {
        try {
          that := StateChannel.this
        } finally {
          changing.set(false)
        }
      }
    }
    val rightToLeft = that.attach { t =>
      if (changing.compareAndSet(false, true)) {
        try {
          StateChannel.this := that
        } finally {
          changing.set(false)
        }
      }
    }
    new Binding(this, that, leftToRight, rightToLeft)
  }
}