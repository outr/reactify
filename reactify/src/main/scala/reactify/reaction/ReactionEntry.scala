package reactify.reaction

/**
  * A reaction's place in a Reactions list. Taking the reaction out marks it dead, which is all a removal costs; the
  * list sweeps its dead entries away later, in one pass.
  */
final class ReactionEntry[T](val reaction: Reaction[T], val key: Int) {
  var live: Boolean = true
}
