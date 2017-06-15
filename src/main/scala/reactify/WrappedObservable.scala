package reactify

class WrappedObservable[T](observables: List[Observable[T]]) extends Observable[T] {
  override def observe(listener: Listener[T]): Listener[T] = {
    observables.foreach(_.observe(listener))
    listener
  }

  override def detach(listener: Listener[T]): Unit = {
    observables.foreach(_.detach(listener))
  }

  override def and(that: Observable[T]): Observable[T] = new WrappedObservable[T](observables ::: List(that))
}