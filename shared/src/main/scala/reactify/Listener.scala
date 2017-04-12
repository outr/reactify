package reactify

trait Listener[T] extends Ordered[Listener[T]] {
  def priority: Double = Listener.Priority.Normal
  def apply(value: T): Unit

  override def compare(that: Listener[T]): Int = priority.compare(that.priority)
}

object Listener {
  object Priority {
    val Lowest: Double = Double.MinValue
    val Low: Double = -100.0
    val Normal: Double = 0.0
    val High: Double = 100.0
    val Highest: Double = Double.MaxValue
  }
}

class FunctionListener[T](f: T => Unit, override val priority: Double) extends Listener[T] {
  override def apply(value: T): Unit = f(value)
}