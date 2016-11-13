package com.outr.props

trait Observable[T] {
  private var observers = Set.empty[T => Unit]

  def attach(f: T => Unit): Unit = synchronized {
    observers += f
  }

  def detach(f: T => Unit): Unit = synchronized {
    observers -= f
  }

  protected def fire(value: T): Unit = observers.foreach { obs =>
    obs(value)
  }
}