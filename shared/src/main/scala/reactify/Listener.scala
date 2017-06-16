package reactify

trait Listener[T] extends Ordered[Listener[T]] {
  def apply(value: T): Unit

  def priority: Double = Listener.Priority.Normal
  override def compare(that: Listener[T]): Int = priority.compare(that.priority)
}

object Listener {
  object Priority {
    val Lowest: Double = Double.MinValue
    val Low: Double = -100.0
    val Normal: Double = 0.0
    val High: Double = 100.0D
    val Highest: Double = Double.MaxValue
  }

  def apply[T](f: T => Unit, priority: Double = Listener.Priority.Normal): Listener[T] = {
    new FunctionListener[T](f, priority)
  }

  private class FunctionListener[T](f: T => Unit, override val priority: Double) extends Listener[T] {
    override def apply(value: T): Unit = f(value)
  }
}