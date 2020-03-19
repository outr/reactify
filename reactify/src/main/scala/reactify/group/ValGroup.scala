package reactify.group

import reactify.Val
import reactify.reaction.Reactions

case class ValGroup[T](items: List[Val[T]]) extends Val[T] with Group[T, Val[T]] {
  override lazy val reactions: Reactions[T] = new GroupReactions[T, Val[T]](this)

  override def and(that: Val[T]): Val[T] = ValGroup(items ::: List(that))

  override def get: T = items.head.get
}
