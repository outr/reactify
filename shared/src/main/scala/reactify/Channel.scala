package reactify

import reactify.group.ChannelGroup
import reactify.standard.StandardChannel

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Channel[T] extends Reactive[T] {
  def set(value: => T): Unit
  def :=(value: => T): Unit = set(value)
  def !(future: Future[T]): Future[Unit] = future.map { value =>
    set(value)
  }

  def and(that: Channel[T]): Channel[T] = ChannelGroup(None, List(this, that))

  def map[R](f: T => R): Channel[R] = {
    val channel = Channel[R]
    attach(channel := f(_))
    channel
  }

  def collect[R](f: PartialFunction[T, R]): Channel[R] = {
    val channel = Channel[R]
    val lifted = f.lift
    attach { t =>
      lifted(t).foreach(v => channel.set(v))
    }
    channel
  }

  override def toString: String = name.getOrElse("Channel")
}

object Channel {
  def apply[T]: Channel[T] = new StandardChannel[T](None)
  def apply[T](name: Option[String]): Channel[T] = new StandardChannel[T](name)
}