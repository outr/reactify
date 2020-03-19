package reactify.group

import reactify.Stateful
import reactify.reaction.Reactions

case class StatefulGroup[T](items: List[Stateful[T]]) extends Stateful[T] with Group[T, Stateful[T]] {
  override lazy val reactions: Reactions[T] = new GroupReactions(this)

  override def get: T = items.head.get
}