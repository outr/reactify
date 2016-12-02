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
  *   val right: Dep[Double, Double] = Dep(left + width, left, _ - width)
  * </code>
  */
class Dep[T, O](_value: () => T, channel: Channel[O], setter: T => O) extends Val[T](_value) with Channel[T] {
  override def set(value: => T): Unit = {
    super.set(value)

    channel := setter(value)
  }
}

object Dep {
  /**
    * Creates a new `Dep` instance for the referenced channel.
    *
    * @param value the derived value for this `Dep`
    * @param channel the channel this instance depends on
    * @param setter conversion from incoming values to this `Dep` to send to the `channel`
    * @tparam T the type of this `Dep`
    * @tparam O the type of the `channel`
    * @return dependency instance
    */
  def apply[T, O](value: => T, channel: Channel[O], setter: T => O): Dep[T, O] = {
    new Dep(() => value, channel, setter)
  }
}