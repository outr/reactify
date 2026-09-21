package reactify

import reactify.reaction.{Reaction, ReactionStatus, Reactions}

import scala.concurrent.{Future, Promise}

/**
  * Reactive is the core trait for Reactify. The basic premise is that a Reactive represents an instance that can attach
  * Reactions and fire `T` and are received by those Reactions.
  *
  * @tparam T the type of value this Reactive receives
  */
trait Reactive[T] {
  private lazy val _status = new ThreadLocal[Option[ReactionStatus]] {
    override def initialValue(): Option[ReactionStatus] = None
  }

  /**
   * If true, events that are fired are synchronized to avoid concurrent invocations. Defaults to false.
   */
  protected def synchronize: Boolean = false

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
  lazy val reactions: Reactions[T] = new Reactions[T]

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
    val sync = synchronize
    if (reactions.isEmpty && !sync && _status.get().isEmpty) {
      // No reactions and no propagation in progress on this thread: the full path would set, read and remove the
      // status without effect. (A fire nested in an active one must still take it: its remove() ends the outer one.)
      ReactionStatus.Continue
    } else if (sync) {
      synchronized(propagate(value, previous, reactions))
    } else {
      // A change that runs through a long chain of values nests a fire per link, and a small device stack (a Roku's is
      // about 120 frames) overruns a dozen links into a layout. Below `maxDepth` the propagation is the plain nested
      // call it always was — same order, same turn; past it a link is queued and run flat once the stack unwinds, by
      // the outermost fire, so an arbitrarily deep chain costs a bounded stack and still finishes in the same turn.
      val d = Reactive.deferral.get()
      if (d.depth >= Reactive.maxDepth) {
        d.queue = d.queue :+ (() => { propagate(value, previous, reactions); () })
        ReactionStatus.Continue
      } else {
        d.depth += 1
        val top = d.depth == 1
        val status = try propagate(value, previous, reactions) finally d.depth -= 1
        if (top) Reactive.drain(d)
        status
      }
    }
  }

  /** The propagation, in one function: the status is Continue while it runs and removed after it, even on an error; each
    * reaction runs in order while the status stays Continue; one that returns Stop, or stops propagation, ends it with
    * Stop. One function rather than a chain of them (and a loop rather than a closure per reaction), because a change
    * that propagates through many values nests a fire per value, and a platform with a small stack (a Roku's is about
    * 120 frames) runs out a few levels into a layout otherwise. */
  private def propagate(value: T, previous: Option[T], reactions: List[Reaction[T]]): ReactionStatus = {
    _status.set(Some(ReactionStatus.Continue))
    try {
      var result: ReactionStatus = null
      var remaining = reactions
      while (result == null && remaining.nonEmpty) {
        val current = _status.get()
        if (current.contains(ReactionStatus.Continue)) {
          val reaction = remaining.head
          val reactionStatus = reaction(value, previous)
          if (!(reactionStatus == ReactionStatus.Continue && _status.get().contains(ReactionStatus.Continue))) {
            result = ReactionStatus.Stop
          }
        } else {
          result = current.getOrElse(throw new RuntimeException("Status not set"))
        }
        remaining = remaining.tail
      }
      if (result == null) _status.get().getOrElse(throw new RuntimeException("Status not set")) else result
    } finally {
      _status.remove()
    }
  }
}

object Reactive {
  /** How deep a chain of fires may nest before the rest runs flat rather than recursively: small enough that a device
    * with a shallow stack does not overrun it, large enough that an ordinary layout never reaches it. */
  private[reactify] val maxDepth: Int = 8

  /** A thread's live propagation depth and the links deferred past the budget, drained by its outermost fire. */
  private[reactify] final class Deferral {
    var depth: Int = 0
    var queue: List[() => Unit] = Nil
  }

  private[reactify] val deferral: ThreadLocal[Deferral] = new ThreadLocal[Deferral] {
    override def initialValue(): Deferral = new Deferral
  }

  /** Run every link deferred past the depth budget, and any deferred while doing so, each with a fresh budget. */
  private[reactify] def drain(d: Deferral): Unit = {
    while (d.queue.nonEmpty) {
      val task = d.queue.head
      d.queue = d.queue.tail
      d.depth += 1
      try task() finally d.depth -= 1
    }
  }

  def fire[T](reactive: Reactive[T], value: T, previous: Option[T], reactions: List[Reaction[T]]): ReactionStatus = {
    reactive.fire(value, previous, reactions)
  }
}
