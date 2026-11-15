package test

import reactify.reaction.{Reaction, Reactions}

/** Reactions of one's own kind, counting what is added. */
class CountingReactions[T] extends Reactions[T] {
  var added: Int = 0

  override def +=(reaction: Reaction[T]): Reaction[T] = {
    added += 1
    super.+=(reaction)
  }
}
