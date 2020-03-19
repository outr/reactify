package reactify

import reactify.group.StatefulGroup
import reactify.reaction.Reaction

trait Stateful[T] extends Reactive[T] {
  /**
    * Gets the current value
    */
  def get: T

  /**
    * Convenience wrapper around `get`
    */
  def apply(): T = get

  /**
    * Convenience functionality to attach a Reaction and immediately fire the current state on the Reaction.
    *
    * @param f the function reaction
    * @param priority the priority in comparison to other reactions (Defaults to Priority.Normal)
    * @return Reaction[T]
    */
  def attachAndFire(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    val reaction = attach(f, priority)
    fire(get, Some(get), List(reaction))
    reaction
  }

  /**
    * Group multiple Statefuls together
    */
  def &(that: Stateful[T]): Stateful[T] = and(that)

  /**
    * Group multiple Statefuls together
    */
  def and(that: Stateful[T]): Stateful[T] = StatefulGroup[T](List(this, that))
}