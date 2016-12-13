package com.outr.props

/**
  * Dep is very much like a `Val`, but is also a `Channel`. The basic purpose is to represent a value dependent upon
  * another variable. An example of this might be if you are representing a position `left` and you also wanted to
  * represent `right` as well (which would be `left` + `width`). These two variables are dependent upon each other and
  * don't fit well as `Var`s. An example usage might be:
  *
  * <code>
  *   val left: Var[Double] = Var(0.0)
  *   val width: Var[Double] = Var(0.0)
  *   val right: Dep = Dep(left, width)
  * </code>
  *
  * If an instance is `submissive` it removes `adjustment` from being part of the mutation dependency. For example: in
  * the above scenario if you set `width` to 100.0 and `right` to 125.0 then `left` will be 25.0. Now, what should happen
  * if you change `width` to 50.0? Should `left` change to 75.0 (`submissive = false`) or should `right` change to 75.0
  * (`submissive = true`)?
  */
class Dep(variable: StateChannel[Double],
          adjustment: => Double,
          submissive: Boolean) extends Val[Double](() => variable + adjustment) with StateChannel[Double] {
  override def set(value: => Double): Unit = {
    super.set(value)

    set(value, submissive)
  }

  def set(value: => Double, submissive: Boolean): Unit = {
    if (submissive) {
      val adj: Double = adjustment
      variable := value - adj
    } else {
      variable := value - adjustment
    }
  }
}

object Dep {
  /**
    * Creates a new `Dep` instance for the referenced channel.
    *
    * @param variable the variable depended on
    * @param adjustment the adjustment to derive the value of the dependency from the variable
    * @param submissive determination of whether setting a new value on the `Dep` will impact continued changes to the
    *                   variable based on this `Dep`'s dependencies or if it should be submissive to the variable. Defaults
    *                   to false.
    *
    * @return dependency instance
    */
  def apply(variable: StateChannel[Double],
            adjustment: => Double,
            submissive: Boolean = false): Dep = {
    new Dep(variable, adjustment, submissive)
  }
}