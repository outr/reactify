package reactify

trait Channel[T] extends Observable[T] {
  /**
    * Convenience method to send a value to `set` similarly to an assignment operator.
    *
    * @param value the value to apply
    */
  def :=(value: => T): Unit = set(value)

  /**
    * Fires the value to all attached listeners.
    *
    * @param value the value to apply
    */
  def set(value: => T): Unit

  override def map[R](f: (T) => R): Channel[R] = super.map(f).asInstanceOf[Channel[R]]

  override def collect[R](f: PartialFunction[T, R]): Channel[R] = super.collect(f).asInstanceOf[Channel[R]]
}

object Channel {
  /**
    * Creates a new Channel.
    */
  def apply[T]: Channel[T] = new Channel[T] {
    override def set(value: => T): Unit = fire(value, InvocationType.Direct)
  }
}