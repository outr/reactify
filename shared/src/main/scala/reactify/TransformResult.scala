package reactify

case class TransformResult[T] private(value: Option[T])