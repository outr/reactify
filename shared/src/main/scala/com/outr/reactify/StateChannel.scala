package com.outr.reactify

trait StateChannel[T] extends Channel[T] with State[T] {
  private var monitoring = List.empty[Observable[_]]
  private val monitorListener = (value: Any) => fire(get)

  override def update(observables: List[Observable[_]], value: => T): Unit = {
    super.update(observables, value)

    monitoring.foreach(_.detach(monitorListener))
    monitoring = observables.distinct.filterNot(_ eq this)
    monitoring.foreach(_.attach(monitorListener))
  }
}