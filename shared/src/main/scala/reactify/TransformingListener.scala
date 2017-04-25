package reactify

trait TransformingListener[T] extends Ordered[TransformingListener[T]] {
  def apply(value: TransformableValue[T]): TransformResult[T]

  def priority: Double = Listener.Priority.Normal
  override def compare(that: TransformingListener[T]): Int = priority.compare(that.priority)
}

object TransformingListener {
  def apply[T](f: TransformableValue[T] => TransformResult[T], priority: Double = Listener.Priority.Normal): TransformingListener[T] = {
    new FunctionTransformingListener[T](f, priority)
  }

  private class FunctionTransformingListener[T](f: TransformableValue[T] => TransformResult[T], override val priority: Double) extends TransformingListener[T] {
    override def apply(value: TransformableValue[T]): TransformResult[T] = f(value)
  }
}