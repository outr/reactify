package reactify

import reactify.group.VarGroup
import reactify.reaction.Reaction
import reactify.standard.StandardVar

trait Var[T] extends Val[T] with Channel[T] {
  def attachAndFire(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    val reaction = attach(f, priority)
    fire(get, Some(get), reactions())
    reaction
  }

  def and(that: Var[T]): Var[T] = VarGroup[T](None, List(this, that))

  override def map[R](f: T => R): Var[R] = {
    val v = Var[R](f(get))
    attach(v := f(_))
    v
  }

  override def toString: String = name.getOrElse("Var")
}

object Var {
  def apply[T](value: => T, name: Option[String] = None): Var[T] = new StandardVar[T](value, name)
}