package com.outr.reactify

import scala.language.experimental.macros

/**
  * Var, as the name suggests, is very similar to a Scala `var`. The value is defined during instantiation, but may be
  * modified later. The value may be a static value, or may be a derived value depending on multiple `Observables`. If
  * `Observables` make up the value they will be monitored and events fired on this `Observable` as the value changes.
  *
  * @tparam T the type of value this channel receives
  */
class Var[T] private() extends StateChannel[T] {
  private var _value: () => T = _

  def this(observables: List[Observable[T]], value: => T) = {
    this()
    update(observables, value)
  }

  override protected def state: T = _value()

  /**
    * Convenience method to pre-evaluate the value instead of as an anonymous function.
    *
    * @param value the value to be set
    */
  def setStatic(value: T): Unit = update(Nil, value)

  /**
    * Convenience method to get the current value.
    */
  def value: T = get

  /**
    * Convenience method to set the current value like a variable.
    */
  def value_=(value: => T): Unit = macro Macros.set

  /**
    * Convenience method to wrap this `Var` into a `Val`.
    */
  override def update(observables: List[Observable[_]], value: => T): Unit = {
    _value = () => value

    super.update(observables, value)
  }
}

object Var {
  /**
    * Creates a new instance of `Var`.
    */
  def apply[T](value: => T): Var[T] = macro Macros.newVar[T]

  /**
    * Convenience method to pre-evaluate the contents as opposed to apply that applies the contents as an anonymous
    * function.
    */
  def static[T](value: T): Var[T] = {
    val v = new Var[T]()
    v.update(Nil, value)
    v
  }
}