package reactify

import reactify.reaction.{Reaction, ReactionStatus, Reactions}
import reactify.standard.StandardReactions

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

/**
  * Reactive is the core trait for Reactify. The basic premise is that a Reactive represents an instance that can attach
  * Reactions and fire instances of `T` that are received by those Reactions.
  *
  * @tparam T the type of value this Reactive receives
  */
trait Reactive[T] {
  /**
    * An optional name associated. This is primarily used for distinguishing between instances as well as logging.
    */
  def name: Option[String]

  private lazy val _status = new ThreadLocal[Option[ReactionStatus]]

  /**
    * If the current thread is reacting to a value currently, status represents the status of the reaction. This can be
    * set to ReactionStatus.Stop in order to stop propagation. This can also be achieved via stopPropagation().
    */
  def status: Option[ReactionStatus] = _status.get()
  def status_=(status: ReactionStatus): Unit = {
    assert(_status.get().nonEmpty, "Cannot set the status without an active reaction on this thread")
    _status.set(Some(status))
  }

  /**
    * Shortcut functionality to call `status = ReactionStatus.Stop`
    */
  def stopPropagation(): Unit = status = ReactionStatus.Stop

  /**
    * Reactions currently associated with this Reactive
    */
  lazy val reactions: Reactions[T] = new StandardReactions[T]

  /**
    * Convenience method to create a Reaction to attach to this Reactive
    *
    * @param f the function reaction
    * @param priority the priority in comparison to other reactions (Defaults to Priority.Normal)
    * @return created Reaction[T]
    */
  def attach(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    reactions += Reaction[T](f, priority)
  }

  /**
    * Convenience method to create a Reaction to monitor changes to this Reactive
    *
    * @param f the function reaction to receive changes
    * @param priority the priority in comparison to other reactions (Defaults to Priority.Normal)
    * @return created Reaction[T]
    */
  def changes(f: (T, T) => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    reactions += Reaction.changes[T](f, priority)
  }

  /**
    * Convenience method to create a Reaction to monitor changes to this Reactive when you don't care about the actual
    * value.
    *
    * @param f the function reaction to invoke in reaction to a value received
    * @param priority the priority in comparison to other reactions (Defaults to Priority.Normal)
    * @return created Reaction[T]
    */
  def on(f: => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    attach(_ => f, priority)
  }

  /**
    * Convenience method to create a Reaction to monitor a single reaction based on an optional condition.
    *
    * @param f the function reaction
    * @param condition optional condition that must be true for this to fire (Defaults to accept anything)
    * @param priority the priority in comparison to other reactions (Defaults to Priority.Normal)
    * @return created Reaction[T]
    */
  def once(f: T => Unit, condition: T => Boolean = _ => true, priority: Double = Priority.Normal): Reaction[T] = {
    var reaction: Reaction[T] = null
    val function = (t: T) => if (condition(t)) {
      reactions -= reaction
      f(t)
    }
    reaction = Reaction[T](function, priority)
    reactions += reaction
    reaction
  }

  /**
    * Convenience method to create a `Future[T]` that will complete upon the next reaction that meets to supplied
    * condition.
    *
    * @param condition optional condition that must be true for this to fire (Defaults to accept anything)
    * @return Future[T]
    */
  def future(condition: T => Boolean = _ => true): Future[T] = {
    val promise = Promise[T]
    once(promise.success, condition)
    promise.future
  }

  /**
    * Fires the value to the Reactions
    */
  protected def fire(value: T, previous: Option[T], reactions: List[Reaction[T]] = this.reactions()): ReactionStatus = {
    _status.set(Some(ReactionStatus.Continue))
    try {
      fireInternal(value, previous, reactions)
    } finally {
      _status.remove()
    }
  }

  @tailrec
  private def fireInternal(value: T,
                           previous: Option[T],
                           reactions: List[Reaction[T]]): ReactionStatus = {
    if (reactions.nonEmpty && status.contains(ReactionStatus.Continue)) {
      val reaction = reactions.head
      val status = reaction(value, previous)
      if (status == ReactionStatus.Continue && this.status.contains(ReactionStatus.Continue)) {
        fireInternal(value, previous, reactions.tail)
      } else {
        ReactionStatus.Stop
      }
    } else {
      status.getOrElse(throw new RuntimeException("Status not set"))
    }
  }
}

object Reactive {
  private[reactify] def fire[T](reactive: Reactive[T], value: T, previous: Option[T]): Unit = {
    reactive.fire(value, previous, reactive.reactions())
  }
}