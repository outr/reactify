package reactify.group

import reactify.Reactive

trait Group[T, R <: Reactive[T]] {
  def items: List[R]
}