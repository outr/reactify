package com.outr.props

/**
  * Var, as the name suggests, is very similar to a Scala `var`. The value is defined during instantiation, but may be
  * modified later. The value may be a static value, or may be a derived value depending on multiple `Observables`. If
  * `Observables` make up the value they will be monitored and events fired on this `Observable` as the value changes.
  *
  * @tparam T the type of value this channel receives
  */
class Var[T] private() extends StateChannel[T] with MonitoringState[T] {
  private var _value: () => T = _

  override protected def state: T = _value()

  /**
    * Sets the value on this `Var`.
    *
    * @param value the value to apply
    */
  override def set(value: => T): Unit = {
    val v = monitor(value)
    _value = () => value
    super.set(v)
  }

  /**
    * Convenience method to pre-evaluate the value instead of as an anonymous function.
    *
    * @param value the value to be set
    */
  def setStatic(value: T): Unit = set(value)

  /**
    * Convenience method to get the current value.
    */
  def value: T = get

  /**
    * Convenience method to set the current value like a variable.
    */
  def value_=(t: => T): Unit = set(t)

  /**
    * Convenience method to wrap this `Var` into a `Val`.
    */
  def toVal: Val[T] = Val(value)
}

object Var {
  /**
    * Creates a new instance of `Var`.
    */
  def apply[T](value: => T): Var[T] = {
    val v = new Var[T]()
    v := value
    v
  }

  /**
    * Convenience method to pre-evaluate the contents as opposed to apply that applies the contents as an anonymous
    * function.
    */
  def static[T](value: T): Var[T] = {
    val v = new Var[T]()
    v := value
    v
  }
}