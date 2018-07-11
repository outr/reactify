package reactify.reaction

/**
  * Reactions represents a list of Reaction instances specifically associated with a Reactive
  */
trait Reactions[T] {
  /**
    * Return all Reactions associated with this Reactive
    */
  def apply(): List[Reaction[T]]

  /**
    * Add a Reaction
    */
  def +=(reaction: Reaction[T]): Reaction[T]

  /**
    * Remove a Reaction
    */
  def -=(reaction: Reaction[T]): Boolean

  /**
    * Remove all Reactions
    */
  def clear(): Unit
}