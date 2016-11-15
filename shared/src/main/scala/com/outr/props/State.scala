package com.outr.props

trait State[T] extends Observable[T] {
  protected def state: T

  def attachAndFire(f: T => Unit): T => Unit = {
    attach(f)
    fire(get)
    f
  }

  def get: T = {
    State.contextFired(this)
    state
  }

  def apply(): T = get

  def asState: State[T] = this
}

object State {
  private val context = new ThreadLocal[Option[StateChannelContext]] {
    override def initialValue(): Option[StateChannelContext] = None
  }

  private[props] def contextualized[R](f: => R): R = {
    val oldValue = context.get()
    context.set(Some(new StateChannelContext))
    try {
      f
    } finally {
      context.set(oldValue)
    }
  }

  private[props] def contextFired[T](channel: State[T]): Unit = context.get().foreach { c =>
    c.observables += channel
  }

  private[props] def contextObservables(): Set[Observable[_]] = context.get().getOrElse(throw new RuntimeException("Not within a context!")).observables
}