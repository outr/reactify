package reactify

/**
  * Var, as the name suggests, is very similar to a Scala `var`. The value is defined during instantiation, but may be
  * modified later. The value may be a static value, or may be a derived value depending on multiple `Observables`. If
  * `Observables` make up the value they will be monitored and events fired on this `Observable` as the value changes.
  *
  * @tparam T the type of value this channel receives
  */
class Var[T](function: () => T,
             distinct: Boolean = true,
             cache: Boolean = true) extends Val[T](function, distinct, cache) with StateChannel[T] {
  override def set(value: => T): Unit = super.set(value)

  /**
    * Convenience method to set the current value like a variable.
    */
  def value_=(value: => T): Unit = set(value)

  def asVal: Val[T] = this

  override def setStatic(value: T): Unit = super.setStatic(value)

  override def toString: String = s"Var($get)"
}

object Var {
  /**
    * Creates a new instance of `Var`.
    */
  def apply[T](value: => T,
               static: Boolean = false,
               distinct: Boolean = true,
               cache: Boolean = true): Var[T] = {
    val f = if (static) {
      val v: T = value
      () => v
    } else {
      () => value
    }
    new Var[T](f, distinct, cache)
  }
}