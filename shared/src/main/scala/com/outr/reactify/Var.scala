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
  private var internal: () => T = _

  def this(observables: List[Observable[T]], value: => T) = {
    this()
    update(observables, value)
  }

  override protected def internalFunction: () => T = internal

  /**
    * Convenience method to wrap this `Var` into a `Val`.
    */
  override def update(observables: List[Observable[_]], value: => T): Unit = {
    internal = () => value

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