package reactify

import reactify.group.ChannelGroup
import reactify.standard.StandardChannel

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Channel is a stateless Reactive implementation exposing a public method to fire values.
  *
  * @tparam T the type of value this Reactive receives
  */
trait Channel[T] extends Reactive[T] {
  /**
    * Public method to fire a value against the Reactions attached to this Channel
    *
    * @param value the function value
    */
  def set(value: => T): Unit

  /**
    * Convenience method to fire a value
    *
    * @see #set
    * @param value the function value
    */
  def :=(value: => T): Unit = set(value)

  /**
    * Convenience method for static (non-functional) invocation.
    *
    * @see #set
    * @param value the value
    */
  def @=(value: T): Unit = set(value)

  /**
    * Convenience functionality to assign the result of a future (upon completion) to this Channel
    */
  def !(future: Future[T]): Future[Unit] = future.map { value =>
    set(value)
  }

  /**
    * Group multiple channels together
    */
  def &(that: Channel[T]): Channel[T] = and(that)

  /**
    * Group multiple channels together
    */
  def and(that: Channel[T]): Channel[T] = ChannelGroup(None, List(this, that))

  /**
    * Functional mapping of this Channel into another Channel. All values received by this Channel will be mapped and
    * forwarded to the new Channel.
    *
    * @param f conversion function
    * @tparam R the type of the new Channel
    * @return Channel[R]
    */
  def map[R](f: T => R): Channel[R] = {
    val channel = Channel[R]
    attach(channel := f(_))
    channel
  }

  /**
    * Functional collection of this Channel into another Channel. All values received by this Channel will be collected
    * and forwarded to the new Channel if they are collected by the conversion function.
    *
    * @param f conversion partial function
    * @tparam R the type of the Channel
    * @return Channel[R]
    */
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