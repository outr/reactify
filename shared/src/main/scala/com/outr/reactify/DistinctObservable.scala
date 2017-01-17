package com.outr.reactify

class DistinctObservable[T](o: Observable[T]) extends Observable[T] {
  private var previous: Option[T] = None

  o match {
    case s: State[_] => previous = Some(s().asInstanceOf[T])
    case _ => // Not a State instance
  }
  o.attach { value =>
    synchronized {
      if (!previous.contains(value)) {
        fire(value)
        previous = Some(value)
      }
    }
  }
}
