package reactify.reaction

/**
  * A reaction's place in a Reactions list. Taking the reaction out marks it dead, which is all a removal costs; the
  * list sweeps its dead entries away later, in one pass.
  */
final class ReactionEntry[T](var reaction: Reaction[T], val key: Int) {
  var live: Boolean = true

  /** Taken out: dead, and holding nothing of the reaction (the entry itself waits in its list for the next sweep). */
  def kill(): Unit = {
    live = false
    reaction = null
  }
}
