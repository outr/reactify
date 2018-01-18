package reactify

/**
  * Convenience functionality for classic change management referencing the old value along with the new value.
  *
  * @tparam T the type of changes
  */
trait ChangeObserver[T] {
  def change(oldValue: T, newValue: T): Unit
}

object ChangeObserver {
  def createFunction[T](observer: ChangeObserver[T], initialValue: Option[T] = None): T => Unit = {
    var previous: Option[T] = initialValue

    (t: T) => {
      previous.foreach(observer.change(_, t))
      previous = Some(t)
    }
  }
}