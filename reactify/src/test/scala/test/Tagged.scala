package test

import reactify.reaction.{Reaction, ReactionStatus}

/** A reaction of one's own making, equal to another by its fields: held by identity, as any reaction but a function's. */
case class Tagged(name: String, heard: String => Unit) extends Reaction[Int] {
  override def apply(value: Int, previous: Option[Int]): ReactionStatus = {
    heard(name)
    ReactionStatus.Continue
  }
}
