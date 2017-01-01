package com.outr.reactify

trait State[T] extends Observable[T] {
  protected def state: T

  def attachAndFire(f: T => Unit): T => Unit = {
    attach(f)
    fire(get)
    f
  }

  override def changes(listener: ChangeListener[T]): (T) => Unit = {
    attach(ChangeListener.createFunction(listener, Some(state)))
  }

  def get: T = state

  def apply(): T = get

  def asState: State[T] = this
}