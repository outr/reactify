package reactify.group

import reactify.Var
import reactify.reaction.Reactions

case class VarGroup[T](items: List[Var[T]]) extends Var[T] with Group[T, Var[T]] {
  override lazy val reactions: Reactions[T] = new GroupReactions[T, Var[T]](this)

  override def set(value: => T): Unit = items.foreach(_.set(value))

  override def and(that: Var[T]): Var[T] = VarGroup(items ::: List(that))

  override def get: T = items.head.get
}