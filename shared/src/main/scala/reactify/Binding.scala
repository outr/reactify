package reactify

import reactify.reaction.Reaction

class Binding[L, R](left: Var[L], right: Var[R], leftToRight: Reaction[L], rightToLeft: Reaction[R]) {
  def detach(): Unit = {
    left.reactions -= leftToRight
    right.reactions -= rightToLeft
  }
}