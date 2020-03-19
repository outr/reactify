package reactify.group

import reactify.Channel
import reactify.reaction.Reactions

case class ChannelGroup[T](items: List[Channel[T]]) extends Channel[T] with Group[T, Channel[T]] {
  override lazy val reactions: Reactions[T] = new GroupReactions(this)

  override def set(value: => T): Unit = items.foreach(_.set(value))

  override def and(that: Channel[T]): Channel[T] = ChannelGroup(items ::: List(that))
}