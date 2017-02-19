package com.outr.reactify

import scala.language.experimental.macros

trait StateChannel[T] extends Channel[T] with State[T] {
  private var monitoring = List.empty[Observable[_]]
  private val monitorListener = (value: Any) => fire(get)

  def observing: List[Observable[_]] = monitoring

  /**
    * Convenience method to pre-evaluate the value instead of as an anonymous function.
    *
    * @param value the value to be set
    */
  def setStatic(value: T): Unit = update(Nil, value)

  /**
    * Convenience method to set the current value like a variable.
    */
  def value_=(value: => T): Unit = macro Macros.set

  override def update(observables: List[Observable[_]], value: => T): Unit = {
    super.update(observables, value)

    monitoring.foreach(_.detach(monitorListener))
    monitoring = observables.distinct.filterNot(_ eq this)
    monitoring.foreach(_.attach(monitorListener))
  }
}