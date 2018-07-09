package reactify

import reactify.standard.StandardChannel

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Channel[T] extends Reactive[T] {
  def set(value: => T): Unit
  def :=(value: => T): Unit = set(value)
  def !(future: Future[T]): Future[Unit] = future.map { value =>
    set(value)
  }

  override def toString: String = name.getOrElse("Channel")
}

object Channel {
  def apply[T]: Channel[T] = new StandardChannel[T](None)
  def apply[T](name: Option[String]): Channel[T] = new StandardChannel[T](name)
}