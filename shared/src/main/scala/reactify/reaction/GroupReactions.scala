package reactify.reaction

import reactify.group.VarGroup

class GroupReactions[T](group: VarGroup[T]) extends Reactions[T](group) {
  override def +=(reaction: Reaction[T]): Reaction[T] = {
    group.vars.foreach(_.reactions += reaction)
    super.+=(reaction)
  }

  override def -=(reaction: Reaction[T]): Boolean = {
    group.vars.foreach(_.reactions -= reaction)
    super.-=(reaction)
  }

  override def clear(): Unit = {
    group._reactions.foreach { r =>
      group.vars.foreach(_.reactions -= r)
    }
    super.clear()
  }
}
