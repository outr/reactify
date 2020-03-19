package reactify

/**
  * Trigger is a convenience class wrapping `Channel[Unit]` specifically for scenarios where the value doesn't matter,
  * just the reactions themselves.
  */
class Trigger extends Channel[Unit] {
  /**
    * Fires an event on this trigger
    */
  def trigger(): Unit = fire((), None)
}

object Trigger {
  def apply(): Trigger = new Trigger
}