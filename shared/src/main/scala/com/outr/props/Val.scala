package com.outr.props

class Val[T] private(value: () => T) extends MonitoringStateChannel[T] {
  monitor(value())

  override protected def state: T = value()
}

object Val {
  def apply[T](value: => T): Val[T] = new Val[T](() => value)
}