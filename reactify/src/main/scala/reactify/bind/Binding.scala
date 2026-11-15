package reactify.bind

import reactify.Var
import reactify.reaction.{Reaction, Reactions}

/**
  * Binding represents a two-way binding between two `Var`s
  */
class Binding[L, R](left: Var[L], right: Var[R], leftToRight: Reaction[L], rightToLeft: Reaction[R]) {
  /**
    * Detaches the binding
    */
  def detach(): Unit = {
    Reactions.remove(left, leftToRight)
    Reactions.remove(right, rightToLeft)
  }
}