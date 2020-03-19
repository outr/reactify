package reactify

import reactify.reaction.{Reaction, ReactionStatus}

/**
  * Dep allows creation of a dependent `Var` on another `Var` allowing conversion between the two. This can be useful for
  * different representations of the same value. For example, in a graphical environment `left`, `center`, and `right`
  * are all different representations of the value (horizontal position). Maintaining three distinct values while
  * keeping them in-sync is painful. With `Dep` you can simply define one `Var` and two `Dep` values like:
  *
  * <code>
  *   val left: Var[Double] = Var(0.0)
  *   val width: Var[Double] = Var(0.0)
  *   val center: Dep[Double, Double] = Dep(left)(_ + (width / 2.0), _ - (width / 2.0))
  *   val right: Dep[Double, Double] = Dep(left)(_ + width, _ - width)
  * </code>
  *
  * Now, modification to `left`, `center`, or `right` will maintain the appropriate value for each without any additional
  * boilerplate.
  *
  * @tparam T the type of value this Reactive receives
  * @tparam R the type that this Dep receives
  */
class Dep[T, R] protected(val owner: Var[R], t2R: T => R, r2T: R => T) extends Reactive[T] with Stateful[T] with Mutable[T] {
  private val v: Val[T] = Val(r2T(owner))

  v.reactions += new Reaction[T] {
    override def apply(value: T, previous: Option[T]): ReactionStatus = {
      fire(value, previous, reactions())
      ReactionStatus.Continue
    }
  }

  override def get: T = v.get

  override def set(f: => T): Unit = owner := t2R(f)
}

object Dep {
  def apply[T, R](owner: Var[R])
                 (implicit r2T: R => T, t2R: T => R): Dep[T, R] = new Dep[T, R](owner, t2R, r2T)
}