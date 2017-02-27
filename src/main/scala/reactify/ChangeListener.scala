package reactify

/**
  * Convenience functionality for classic change management referencing the old value along with the new value.
  *
  * @tparam T the type of changes
  */
trait ChangeListener[T] {
  def change(oldValue: T, newValue: T): Unit
}

object ChangeListener {
  def createFunction[T](listener: ChangeListener[T], initialValue: Option[T] = None): T => Unit = {
    var previous: Option[T] = initialValue

    (t: T) => {
      previous.foreach(listener.change(_, t))
      previous = Some(t)
    }
  }
}