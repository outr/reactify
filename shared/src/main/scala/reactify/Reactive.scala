package reactify

import reactify.reaction.{Reaction, ReactionStatus, Reactions}

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

trait Reactive[T] {
  def name: Option[String]
  private[reactify] var _reactions = List.empty[Reaction[T]]
  private lazy val _status = new ThreadLocal[Option[ReactionStatus]]

  def status: Option[ReactionStatus] = _status.get()
  def status_=(status: ReactionStatus): Unit = {
    assert(_status.get().nonEmpty, "Cannot set the status without an active reaction on this thread")
    _status.set(Some(status))
  }

  def stopPropagation(): Unit = status = ReactionStatus.Stop

  lazy val reactions = new Reactions[T](this)

  def attach(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    reactions += Reaction[T](f, priority)
  }

  def changes(f: (T, T) => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    reactions += Reaction.changes[T](f, priority)
  }

  def on(f: => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    attach(_ => f, priority)
  }

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

  def future(condition: T => Boolean = _ => true): Future[T] = {
    val promise = Promise[T]
    once(promise.success, condition)
    promise.future
  }

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
  def fire[T](reactive: Reactive[T], value: T, previous: Option[T]): Unit = {
    reactive.fire(value, previous, reactive.reactions())
  }
}