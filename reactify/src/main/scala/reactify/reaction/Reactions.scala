package reactify.reaction

/**
  * Reactions represents a list of Reaction instances specifically associated with a Reactive
  */
class Reactions[T] {
  private var list = List.empty[Reaction[T]]

  /**
    * Return all Reactions associated with this Reactive
    */
  def apply(): List[Reaction[T]] = list

  /**
    * Add a Reaction
    */
  def +=(reaction: Reaction[T]): Reaction[T] = synchronized {
    list = (list ::: List(reaction)).sorted.distinct
    reaction
  }

  /**
    * Remove a Reaction
    */
  def -=(reaction: Reaction[T]): Boolean = synchronized {
    val previous = list
    list = list.filterNot(_ eq reaction)
    previous != list
  }

  /**
    * Remove all Reactions
    */
  def clear(): Unit = synchronized {
    list = Nil
  }
}
