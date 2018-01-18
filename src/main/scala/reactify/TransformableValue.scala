package reactify

case class TransformableValue[T](value: T) {
  def continue: TransformResult[T] = TransformResult[T](Some(value))
  def cancel: TransformResult[T] = TransformResult[T](None)
  def transform(newValue: T): TransformResult[T] = TransformResult[T](Some(newValue))
}