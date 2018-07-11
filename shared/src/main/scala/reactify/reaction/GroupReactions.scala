package reactify.reaction

import reactify.Reactive
import reactify.group.Group
import reactify.standard.StandardReactions

class GroupReactions[T, R <: Reactive[T]](group: Group[T, R]) extends StandardReactions[T] {
  override def +=(reaction: Reaction[T]): Reaction[T] = {
    group.items.foreach(_.reactions += reaction)
    super.+=(reaction)
  }

  override def -=(reaction: Reaction[T]): Boolean = {
    group.items.foreach(_.reactions -= reaction)
    super.-=(reaction)
  }

  override def clear(): Unit = {
    apply().foreach { r =>
      group.items.foreach(_.reactions -= r)
    }
    super.clear()
  }
}