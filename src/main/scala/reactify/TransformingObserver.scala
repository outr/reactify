package reactify

trait TransformingObserver[T] extends Ordered[TransformingObserver[T]] {
  def apply(value: TransformableValue[T]): TransformResult[T]

  def priority: Double = Observer.Priority.Normal
  override def compare(that: TransformingObserver[T]): Int = priority.compare(that.priority)
}

object TransformingObserver {
  def apply[T](f: TransformableValue[T] => TransformResult[T], priority: Double = Observer.Priority.Normal): TransformingObserver[T] = {
    new FunctionTransformingObserver[T](f, priority)
  }

  private class FunctionTransformingObserver[T](f: TransformableValue[T] => TransformResult[T], override val priority: Double) extends TransformingObserver[T] {
    override def apply(value: TransformableValue[T]): TransformResult[T] = f(value)
  }
}