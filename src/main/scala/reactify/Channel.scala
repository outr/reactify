package reactify

import scala.concurrent.Future

trait Channel[T] extends Observable[T] {
  /**
    * Convenience method to send a value to `set` similarly to an assignment operator.
    *
    * @param value the value to apply
    */
  def :=(value: => T): Unit = set(value)

  /**
    * Convenience method to send the result of a Future to this channel upon success.
    *
    * @param future the future to monitor
    * @return mapped `future` to conclude upon setting the value to this channel
    */
  def !(future: Future[T]): Future[Unit] = future.map { value =>
    set(value)
  }

  /**
    * Fires the value to all attached observers.
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