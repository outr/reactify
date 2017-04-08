package reactify

import scala.concurrent.Future

/**
  * Trigger works very similar to `Channel` except it doesn't receive anything. Listeners are attached like on an
  * `Observable`, but they receive no arguments.
  */
class Trigger {
  private val channel = Channel[Unit]

  /**
    * Attaches a function to listen for triggering.
    *
    * @param f function listener
    * @return the listener instance
    */
  def attach(f: => Unit): Unit => Unit = channel.attach(_ => f)

  /**
    * Detaches a listener from this Trigger.
    *
    * @param f the listener to detach
    */
  def detach(f: Unit => Unit): Unit = channel.detach(f)

  /**
    * Attaches the listener but automatically detaches after being invoked once.
    *
    * @param f the listener
    */
  def once(f: => Unit): Unit = channel.once(_ => f)

  /**
    * Future that completes successfully upon the next triggering of this Trigger.
    */
  def future(): Future[Unit] = channel.future()

  /**
    * Triggers this instance.
    */
  def fire(): Unit = channel.fire(())

  /**
    * Clears all attached listeners.
    */
  def clear(): Unit = channel.clear()

  /**
    * Cleans up all cross references in preparation for GC.
    */
  def dispose(): Unit = channel.dispose()
}