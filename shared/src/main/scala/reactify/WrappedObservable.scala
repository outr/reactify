package reactify

class WrappedObservable[T](observables: List[Observable[T]]) extends Observable[T] {
  override def observe(observer: Observer[T]): Observer[T] = {
    observables.foreach(_.observe(observer))
    observer
  }

  override def detach(observer: Observer[T]): Unit = {
    observables.foreach(_.detach(observer))
  }

  override def and(that: Observable[T]): Observable[T] = new WrappedObservable[T](observables ::: List(that))
}