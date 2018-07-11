package reactify

import reactify.standard.StandardChannel

/**
  * Trigger is a convenience class wrapping `Channel[Unit]` specifically for scenarios where the value doesn't matter,
  * just the reactions themselves.
  */
trait Trigger extends Channel[Unit] {
  def trigger(): Unit = fire((), None)
}

object Trigger {
  def apply(): Trigger = new StandardChannel[Unit](None) with Trigger
  def apply(name: String): Trigger = new StandardChannel[Unit](Option(name)) with Trigger
}