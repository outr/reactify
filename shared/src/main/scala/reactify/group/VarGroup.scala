package reactify.group

import reactify.reaction.{GroupReactions, Reactions}
import reactify.{State, Var}

case class VarGroup[T](name: Option[String], vars: List[Var[T]]) extends Var[T] {
  override lazy val reactions: Reactions[T] = new GroupReactions[T](this)

  override def set(value: => T): Unit = vars.foreach(_.set(value))

  override def state: State[T] = ???

  override def and(v: Var[T]): Var[T] = VarGroup(name, vars ::: List(v))
}