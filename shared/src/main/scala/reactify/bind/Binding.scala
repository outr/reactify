package reactify.bind

import reactify.Var
import reactify.reaction.Reaction

/**
  * Binding represents a two-way binding between two `Var`s
  */
class Binding[L, R](left: Var[L], right: Var[R], leftToRight: Reaction[L], rightToLeft: Reaction[R]) {
  /**
    * Detaches the binding
    */
  def detach(): Unit = {
    left.reactions -= leftToRight
    right.reactions -= rightToLeft
  }
}