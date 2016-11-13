package com.outr.props

class Channel[T] extends Observable[T] {
  def :=(value: => T): Unit = fire(value)
}

object Channel {
  def apply[T](): Channel[T] = new Channel[T]
}