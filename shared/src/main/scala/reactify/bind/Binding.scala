package reactify.bind

import reactify._

class Binding[L, R](left: StateChannel[L], right: StateChannel[R], leftToRight: Observer[L], rightToLeft: Observer[R]) {
  def detach(): Unit = {
    left.detach(leftToRight)
    right.detach(rightToLeft)
  }
}