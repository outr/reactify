package com.outr.props

trait Channel[T] extends Observable[T] {
  def :=(value: => T): Unit = fire(value)
}