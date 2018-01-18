package reactify

trait Observer[T] extends Ordered[Observer[T]] {
  def apply(value: T, `type`: InvocationType): Unit

  def priority: Double = Observer.Priority.Normal
  override def compare(that: Observer[T]): Int = priority.compare(that.priority)
}

object Observer {
  object Priority {
    val Lowest: Double = Double.MinValue
    val Low: Double = -100.0
    val Normal: Double = 0.0
    val High: Double = 100.0D
    val Highest: Double = Double.MaxValue
  }

  def apply[T](f: T => Unit, priority: Double = Observer.Priority.Normal): Observer[T] = {
    new FunctionObserver[T](f, priority)
  }

  private class FunctionObserver[T](f: T => Unit, override val priority: Double) extends Observer[T] {
    override def apply(value: T, `type`: InvocationType): Unit = f(value)
  }
}