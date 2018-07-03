package reactify

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Channel[T] extends Reactive[T] {
  def set(value: => T): Unit
  def :=(value: => T): Unit = set(value)
  def !(future: Future[T]): Future[Unit] = future.map { value =>
    set(value)
  }
}

object Channel {
  def apply[T]: Channel[T] = new Channel[T] {
    override def set(value: => T): Unit = fire(value, reactions())
  }
}