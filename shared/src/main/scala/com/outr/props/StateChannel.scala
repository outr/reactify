package com.outr.props

trait StateChannel[T] extends Channel[T] with State[T] {
  def mod(f: T => T): Unit = {
    val current: T = get
    set(f(current))
  }
}