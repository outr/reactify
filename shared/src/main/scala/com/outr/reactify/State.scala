package com.outr.reactify

trait State[T] extends Observable[T] {
  protected def internalFunction: () => T

  def attachAndFire(f: T => Unit): T => Unit = {
    attach(f)
    fire(get)
    f
  }

  override def changes(listener: ChangeListener[T]): (T) => Unit = {
    attach(ChangeListener.createFunction(listener, Some(internalFunction())))
  }

  def get: T = internalFunction()

  /**
    * Convenience method to get the current value.
    */
  def value: T = get

  def apply(): T = get
}

object State {
  def internalFunction[T](state: State[T]): () => T = state.internalFunction
}