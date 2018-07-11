package reactify.group

import reactify.Reactive

/**
  * Group represents a simple grouping of multiple underlying instances
  */
trait Group[T, R <: Reactive[T]] {
  def items: List[R]
}