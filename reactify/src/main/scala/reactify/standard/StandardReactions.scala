package reactify.standard

import reactify.reaction.{Reaction, Reactions}

class StandardReactions[T] extends Reactions[T] {
  var list = List.empty[Reaction[T]]

  def apply(): List[Reaction[T]] = list

  def +=(reaction: Reaction[T]): Reaction[T] = synchronized {
    list = (list ::: List(reaction)).sorted.distinct
    reaction
  }

  def -=(reaction: Reaction[T]): Boolean = synchronized {
    val previous = list
    list = list.filterNot(_ eq reaction)
    previous != list
  }

  def clear(): Unit = synchronized {
    list = Nil
  }
}
