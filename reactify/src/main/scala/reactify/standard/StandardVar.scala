package reactify.standard

import reactify.Var

class StandardVar[T](f: => T, override val mode: Var.Mode, name: Option[String]) extends StandardVal[T](f, name) with Var[T] {
  override def set(value: => T): Unit = set(value, mode)

  override def set(value: => T, mode: Var.Mode): Unit = refresh(value, mode)
}