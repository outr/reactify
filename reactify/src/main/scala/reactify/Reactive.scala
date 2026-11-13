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
  /**
   * If true, events that are fired are synchronized to avoid concurrent invocations. Defaults to false.
   */
  protected def synchronize: Boolean = false

  /** Whether `value`, fired a while ago, is no longer this one's: a value that has moved on since. A channel's every
    * event counts, so a channel never says so. */
  protected def superseded(value: T): Boolean = false

  /**
    * If the current thread is reacting to a value currently, status represents the status of the reaction. This can be
    * set to ReactionStatus.Stop in order to stop propagation. This can also be achieved via stopPropagation().
    */
  def status: Option[ReactionStatus] = {
    val p = Reactive.propagationOf(this, Reactive.propagations.get())
    if (p == null || p.status == null) None else Some(p.status)
  }
  def status_=(status: ReactionStatus): Unit = {
    val p = Reactive.propagationOf(this, Reactive.propagations.get())
    assert(p != null && p.status != null, "Cannot set the status without an active reaction on this thread")
    p.status = status
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
    val c = Reactive.propagations.get()
    if (reactions.isEmpty && !sync && (c.innermost == null || Reactive.propagationOf(this, c) == null)) {
      // No reactions and no propagation of this value in progress on this thread: the full path would begin and end a
      // propagation without effect. (A fire nested in an active one must still take it: its end ends the outer one.)
      ReactionStatus.Continue
    } else if (sync) {
      synchronized(propagate(value, previous, reactions, c))
    } else {
      // A change that runs through a long chain of values nests a fire per link, and a small device stack (a Roku's is
      // about 120 frames) overruns a dozen links into a layout. Below `maxDepth` the propagation is the plain nested
      // call it always was — same order, same turn; past it a link is queued and run flat once the stack unwinds, by
      // the outermost fire, so an arbitrarily deep chain costs a bounded stack and still finishes in the same turn.
      if (c.depth >= Reactive.maxDepth) {
        // unless the value moves on before the link runs: its newer value, fired nearer the top, has reached the
        // reactions already (or is queued behind this), and this, run after it, would put the older value back
        c.queue = c.queue :+ (() => if (!superseded(value)) { propagate(value, previous, reactions, c); () })
        ReactionStatus.Continue
      } else {
        c.depth += 1
        val top = c.depth == 1
        val status = try propagate(value, previous, reactions, c) finally c.depth -= 1
        if (top) Reactive.drain(c)
        status
      }
    }
  }

  /** The propagation, in one function: the status is Continue while it runs and gone after it, even on an error; each
    * reaction runs in order while the status stays Continue; one that returns Stop, or stops propagation, ends it with
    * Stop. One function rather than a chain of them (and a loop rather than a closure per reaction), because a change
    * that propagates through many values nests a fire per value, and a platform with a small stack (a Roku's is about
    * 120 frames) runs out a few levels into a layout otherwise.
    *
    * The status lives in the propagation, on the thread's list of those under way, put there here and taken off at
    * the end, rather than in a thread-local of each value: one thread-local for the thread, and none made for each
    * value. A value has one status on a thread, so a propagation of it nested in one under way (a reaction that sets the
    * value again) ends the outer one with it: the outer stops, and its later reactions do not hear the older value
    * after the newer. */
  private def propagate(value: T, previous: Option[T], reactions: List[Reaction[T]],
                        c: Reactive.Propagations): ReactionStatus = {
    val p = new Reactive.Propagation(this, ReactionStatus.Continue, c.innermost)
    c.innermost = p
    try {
      var stopped = false
      var remaining = reactions
      while (!stopped && remaining.nonEmpty) {
        val reaction = remaining.head
        val reactionStatus = reaction(value, previous)
        stopped = !(reactionStatus == ReactionStatus.Continue && p.status == ReactionStatus.Continue)
        remaining = remaining.tail
      }
      if (stopped) ReactionStatus.Stop else ReactionStatus.Continue
    } finally {
      c.innermost = p.outer
      if (c.innermost != null) {
        val outer = Reactive.propagationOf(this, c)
        if (outer != null) outer.status = null
      }
    }
  }
}

object Reactive {
  /** How deep a chain of fires may nest before the rest runs flat rather than recursively: small enough that a device
    * with a shallow stack does not overrun it, large enough that an ordinary layout never reaches it. */
  private[reactify] val maxDepth: Int = 8

  /** A thread's propagations: its live depth and the links deferred past the budget, drained by its outermost fire, and
    * the innermost propagation under way (each holding the one it is nested in). */
  private[reactify] final class Propagations {
    var depth: Int = 0
    var queue: List[() => Unit] = Nil
    var innermost: Propagation = null
  }

  /** A propagation of `reactive`'s value under way: its status, null once a nested propagation of the same value has
    * ended it. */
  private[reactify] final class Propagation(val reactive: Reactive[_], var status: ReactionStatus, val outer: Propagation)

  private[reactify] val propagations: ThreadLocal[Propagations] = new ThreadLocal[Propagations] {
    override def initialValue(): Propagations = new Propagations
  }

  /** The innermost propagation of `reactive` under way on the thread whose propagations `c` are, or null. */
  private[reactify] def propagationOf(reactive: Reactive[_], c: Propagations): Propagation = {
    var p = c.innermost
    while (p != null && !(p.reactive eq reactive)) p = p.outer
    p
  }

  /** Run every link deferred past the depth budget, and any deferred while doing so, each with a fresh budget. */
  private[reactify] def drain(c: Propagations): Unit = {
    while (c.queue.nonEmpty) {
      val task = c.queue.head
      c.queue = c.queue.tail
      c.depth += 1
      try task() finally c.depth -= 1
    }
  }

  def fire[T](reactive: Reactive[T], value: T, previous: Option[T], reactions: List[Reaction[T]]): ReactionStatus = {
    reactive.fire(value, previous, reactions)
  }
}
