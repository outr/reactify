package reactify.group

import reactify.Reactive
import reactify.reaction.{Reaction, Reactions}

class GroupReactions[T, R <: Reactive[T]](group: Group[T, R]) extends Reactions[T] {
  override def +=(reaction: Reaction[T]): Reaction[T] = {
    group.items.foreach(item => Reactions.add(item, reaction))
    super.+=(reaction)
  }

  override def -=(reaction: Reaction[T]): Boolean = {
    group.items.foreach(item => Reactions.remove(item, reaction))
    super.-=(reaction)
  }

  override def clear(): Unit = {
    apply().foreach { r =>
      group.items.foreach(item => Reactions.remove(item, r))
    }
    super.clear()
  }
}
