package test

import reactify.{Reactive, Var}
import reactify.reaction.{Reaction, ReactionStatus}

/** A value whose reactions are its own kind, which it fires itself as what it follows changes. */
class OwnReactionsProbe(inner: Var[Int]) extends Reactive[Int] {
  override lazy val reactions: CountingReactions[Int] = new CountingReactions[Int]

  inner.reactions += new Reaction[Int] {
    override def apply(value: Int, previous: Option[Int]): ReactionStatus = {
      fire(value, previous, reactions())
      ReactionStatus.Continue
    }
  }

  def added: Int = reactions.added
}
