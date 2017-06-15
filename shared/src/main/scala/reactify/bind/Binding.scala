package reactify.bind

import reactify._

class Binding[L, R](left: StateChannel[L], right: StateChannel[R], leftToRight: Listener[L], rightToLeft: Listener[R]) {
  def detach(): Unit = {
    left.detach(leftToRight)
    right.detach(rightToLeft)
  }
}