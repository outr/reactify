package reactify

class Binding[T](left: StateChannel[T], right: StateChannel[T], leftToRight: Listener[T], rightToLeft: Listener[T]) {
  def detach(): Unit = {
    left.detach(leftToRight)
    right.detach(rightToLeft)
  }
}
