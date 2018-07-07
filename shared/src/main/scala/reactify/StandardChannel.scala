package reactify

class StandardChannel[T](val name: Option[String]) extends Channel[T] {
  override def set(value: => T): Unit = fire(value, None, reactions())
}
