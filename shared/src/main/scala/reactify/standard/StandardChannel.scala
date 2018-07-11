package reactify.standard

import reactify.Channel

class StandardChannel[T](val name: Option[String]) extends Channel[T] {
  override def set(value: => T): Unit = fire(value, None, reactions())
}
