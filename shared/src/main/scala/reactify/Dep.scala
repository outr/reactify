package reactify

/**
  * Dep is very much like a `Val`, but is also a `Channel`. The basic purpose is to represent a value dependent upon
  * another variable. An example of this might be if you are representing a position `left` and you also wanted to
  * represent `right` as well (which would be `left` + `width`). These two variables are dependent upon each other and
  * don't fit well as `Var`s. An example usage might be:
  *
  * <code>
  *   val left: Var[Double] = Var(0.0)
  *   val width: Var[Double] = Var(0.0)
  *   val right: Dep[Double, Double] = Dep(left, width)
  * </code>
  *
  * If an instance is `submissive` it removes `adjustment` from being part of the mutation dependency. For example: in
  * the above scenario if you set `width` to 100.0 and `right` to 125.0 then `left` will be 25.0. Now, what should happen
  * if you change `width` to 50.0? Should `left` change to 75.0 (`submissive = false`) or should `right` change to 75.0
  * (`submissive = true`)?
  */
class Dep[T, V] private(variable: Var[V],
                        adjustment: => T,
                        submissive: Boolean)
                       (implicit connector: DepConnector[T, V]) extends Var[T](() => connector.combine(variable(), adjustment)) {
  variable.attach { value =>
    val adjusted = connector.combine(value, adjustment)
    if (get != adjusted) {
      set(adjusted)
    }
  }

  override protected[reactify] def fire(value: T): Unit = {
    super.fire(value)

    val adjusted = connector.extract(value, adjustment)
    if (variable() != adjusted) {
      variable := adjusted
    }
  }
}

object Dep {
  /**
    * Creates a new `Dep` instance for the referenced channel.
    *
    * @param variable the variable depended on
    * @param adjustment the adjustment to derive the value of the dependency from the variable
    * @param submissive whether this Dep should submit to the variable.
    *
    * @return dependency instance
    */
  def apply[T, V](variable: Var[V],
                  adjustment: => T,
                  submissive: Boolean = false)
                 (implicit connector: DepConnector[T, V]): Dep[T, V] = {
    new Dep[T, V](variable, adjustment, submissive)
  }
}