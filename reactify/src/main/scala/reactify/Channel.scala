package reactify

import reactify.group.ChannelGroup

/**
  * Channel is a stateless Reactive implementation exposing a public method to fire values.
  *
  * @tparam T the type of value this Reactive receives
  */
class Channel[T] extends Reactive[T] with Mutable[T] {
  override def set(f: => T): Unit = fire(f, None, reactions())

  /**
    * Group multiple channels together
    */
  def &(that: Channel[T]): Channel[T] = and(that)

  /**
    * Group multiple channels together
    */
  def and(that: Channel[T]): Channel[T] = ChannelGroup(List(this, that))

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
}

object Channel {
  def apply[T]: Channel[T] = new Channel[T]
}