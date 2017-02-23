package reactify

/**
  * Var, as the name suggests, is very similar to a Scala `var`. The value is defined during instantiation, but may be
  * modified later. The value may be a static value, or may be a derived value depending on multiple `Observables`. If
  * `Observables` make up the value they will be monitored and events fired on this `Observable` as the value changes.
  *
  * @tparam T the type of value this channel receives
  */
class Var[T] private(function: () => T) extends State[T](function) with Channel[T] {
  override def set(value: => T): Unit = super.set(value)

  override def :=(value: => T): Unit = set(value)

  /**
    * Convenience method to set the current value like a variable.
    */
  def value_=(value: => T): Unit = set(value)

  override def setStatic(value: T): Unit = super.setStatic(value)
}

object Var {
  /**
    * Creates a new instance of `Var`.
    */
  def apply[T](value: => T): Var[T] = new Var[T](() => value)

  /**
    * Convenience method to pre-evaluate the contents as opposed to apply that applies the contents as an anonymous
    * function.
    */
  def static[T](value: => T): Var[T] = {
    val v: T = value
    apply[T](v)
  }
}