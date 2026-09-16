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
    // The list is kept sorted by priority and distinct, so the reaction goes where a stable sort of the list with it
    // appended would put it (after every reaction that does not compare above it), unless an equal one is already
    // there. One pass, rather than sorting and de-duplicating the whole list on every attach, which a platform whose
    // sort and distinct are quadratic (a Roku) pays for heavily when a screen attaches hundreds of reactions.
    // Most reactions share a priority, and a new one then goes last: that is known from the last alone, without a
    // second pass to place it.
    if (!list.contains(reaction)) {
      if (list.isEmpty || list.last.compare(reaction) <= 0) list = list :+ reaction
      else {
        var at = 0
        var placing = true
        list.foreach { r =>
          if (placing && r.compare(reaction) <= 0) at += 1
          else placing = false
        }
        list = list.take(at) ::: (reaction :: list.drop(at))
      }
    }
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
