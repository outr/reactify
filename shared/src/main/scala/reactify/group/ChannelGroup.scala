package reactify.group

import reactify.Channel

case class ChannelGroup[T](override val name: Option[String],
                           items: List[Channel[T]]) extends Channel[T] with Group[T, Channel[T]] {
  override def set(value: => T): Unit = items.foreach(_.set(value))

  override def and(that: Channel[T]): Channel[T] = ChannelGroup(name, items ::: List(that))
}