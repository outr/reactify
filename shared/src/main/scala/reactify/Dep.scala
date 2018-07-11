package reactify

import reactify.standard.StandardDep

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
trait Dep[T, R] extends Var[T] {
  def owner: Var[R]
  def t2R(t: T): R
  def r2T(r: R): T
}

object Dep {
  def apply[T, R](owner: Var[R])
                 (implicit r2T: R => T, t2R: T => R): Dep[T, R] = new StandardDep[T, R](None, owner, r2T, t2R)
  def apply[T, R](owner: Var[R],
                  name: String)
                 (implicit r2T: R => T, t2R: T => R): Dep[T, R] = new StandardDep[T, R](Option(name), owner, r2T, t2R)
}