package com.outr.props

class Var[T] private() extends Channel[T] with MonitoringStateChannel[T] {
  private var _value: () => T = _

  override protected def state: T = _value()

  override def :=(value: => T): Unit = {
    val v = monitor(value)
    _value = () => value
    super.:=(v)
  }
}

object Var {
  def apply[T](value: => T): Var[T] = {
    val v = new Var[T]()
    v := value
    v
  }
}