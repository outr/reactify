package reactify

import scala.concurrent.Future

/**
  * Trigger works very similar to `Channel` except it doesn't receive anything. `Observers` are attached like on an
  * `Observable`, but they receive no arguments.
  */
class Trigger {
  private val channel = Channel[Unit]

  /**
    * Attaches a function to listen for triggering.
    *
    * @param f function observer
    * @return the observer instance
    */
  def attach(f: => Unit): Observer[Unit] = channel.attach(_ => f)

  /**
    * Detaches an observer from this Trigger.
    *
    * @param f the observer to detach
    */
  def detach(f: Observer[Unit]): Unit = channel.detach(f)

  /**
    * Attaches the observer but automatically detaches after being invoked once.
    *
    * @param f the observer
    */
  def once(f: => Unit): Unit = channel.once(_ => f)

  /**
    * Future that completes successfully upon the next triggering of this Trigger.
    */
  def future(): Future[Unit] = channel.future()

  /**
    * Triggers this instance.
    */
  def fire(): Unit = channel.fire((), InvocationType.Direct)

  /**
    * Clears all attached observers.
    */
  def clearObservers(): Unit = channel.clearObservers()

  /**
    * Cleans up all cross references in preparation for GC.
    */
  def dispose(): Unit = channel.dispose()
}

object Trigger {
  def apply(): Trigger = new Trigger
}