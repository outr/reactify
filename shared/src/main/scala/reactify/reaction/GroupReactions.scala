package reactify.reaction

import reactify.group.VarGroup
import reactify.standard.StandardReactions

class GroupReactions[T](group: VarGroup[T]) extends StandardReactions[T] {
  override def +=(reaction: Reaction[T]): Reaction[T] = {
    group.vars.foreach(_.reactions += reaction)
    super.+=(reaction)
  }

  override def -=(reaction: Reaction[T]): Boolean = {
    group.vars.foreach(_.reactions -= reaction)
    super.-=(reaction)
  }

  override def clear(): Unit = {
    apply().foreach { r =>
      group.vars.foreach(_.reactions -= r)
    }
    super.clear()
  }
}
