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
    if (!list.contains(reaction)) place(reaction)
    reaction
  }

  /** Add a reaction known not to be here already (a Val's own, on a value it has just come to depend on): the check
    * for one is a pass over every reaction, and a value every screen depends on (a theme colour) has hundreds. */
  private[reactify] def append(reaction: Reaction[T]): Unit = synchronized(place(reaction))

  private def place(reaction: Reaction[T]): Unit = {
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

  /**
    * Remove a Reaction
    */
  def -=(reaction: Reaction[T]): Boolean = synchronized {
    val before = list.length
    list = list.filterNot(_ eq reaction)
    list.length != before
  }

  /**
    * Remove all Reactions
    */
  def clear(): Unit = synchronized {
    list = Nil
  }
}
