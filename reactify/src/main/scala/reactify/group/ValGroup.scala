package reactify.group

import reactify.reaction.{GroupReactions, Reactions}
import reactify.Val

case class ValGroup[T](override val name: Option[String], items: List[Val[T]]) extends Val[T] with Group[T, Val[T]] {
  override lazy val reactions: Reactions[T] = new GroupReactions(this)

  override def get: T = items.head.get
}