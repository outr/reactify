package reactify

class StandardChannel[T] extends Channel[T] {
  override def set(value: => T): Unit = fire(value, None, reactions())
}
