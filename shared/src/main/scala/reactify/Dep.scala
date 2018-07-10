package reactify

import reactify.reaction.{Reaction, ReactionStatus}
import reactify.standard.StandardDep

trait Dep[T, R] extends Var[T] with Reaction[R] {
  def owner: Var[R]
  def t2R(t: T): R
  def r2T(r: R): T

  // TODO: Create DepState for reference internalization

  owner.reactions += this

  override def set(value: => T): Unit = owner := t2R(value)

  override def get: T = r2T(owner())

  override def state: State[T] = ???

  override def apply(value: R, previous: Option[R]): ReactionStatus = {
    val c = r2T(value)
    val p = previous.map(r2T)
    fire(c, p)
  }
}

object Dep {
  def apply[T, R](owner: Var[R])
                 (implicit t2R: T => R, r2T: R => T): Dep[T, R] = new StandardDep[T, R](None, owner, t2R, r2T)
  def apply[T, R](owner: Var[R],
                  name: String)
                 (implicit t2R: T => R, r2T: R => T): Dep[T, R] = new StandardDep[T, R](Option(name), owner, t2R, r2T)
}