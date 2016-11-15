package com.outr.props

/**
  * Val, as the name suggests, is like a Scala `val`. This represents an immutable value that is set in the first place
  * and then not modified. However, since the value set may be built from `Observables`, the generated value may change
  * over time as its dependencies are modified. This class is `Observable` and will only fire changes if the underlying
  * value is derived from one or more `Observables`.
  *
  * @tparam T the type of value retained by this `State`
  */
class Val[T] private(_value: () => T) extends MonitoringState[T] {
  monitor(_value())

  override protected def state: T = _value()

  /**
    * Convenience method to get the current value.
    */
  def value: T = get

  /**
    * Convenience method to wrap this `Val` into a `Var`.
    */
  def toVar(): Var[T] = Var(value)
}

object Val {
  /**
    * Creates a new instance of a `Val[T]`
    */
  def apply[T](value: => T): Val[T] = new Val[T](() => value)

  /**
    * Convenience method to pre-evaluate the contents as opposed to apply that applies the contents as an anonymous
    * function.
    */
  def static[T](value: T): Val[T] = new Val[T](() => value)
}