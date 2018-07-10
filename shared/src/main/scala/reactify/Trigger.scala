package reactify

import reactify.standard.StandardChannel

trait Trigger extends Channel[Unit] {
  def trigger(): Unit = fire((), None)
}

object Trigger {
  def apply(): Trigger = new StandardChannel[Unit](None) with Trigger
  def apply(name: String): Trigger = new StandardChannel[Unit](Option(name)) with Trigger
}