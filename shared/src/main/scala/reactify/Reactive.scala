package reactify

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

trait Reactive[T] {
  def name: Option[String]
  private[reactify] var _reactions = List.empty[Reaction[T]]

  object reactions {
    def apply(): List[Reaction[T]] = _reactions

    def +=(reaction: Reaction[T]): Reaction[T] = synchronized {
      _reactions = (_reactions ::: List(reaction)).sorted.distinct
      reaction
    }

    def -=(reaction: Reaction[T]): Boolean = synchronized {
      val previous = _reactions
      _reactions = _reactions.filterNot(_ eq reaction)
      previous != _reactions
    }

    def clear(): Unit = synchronized {
      _reactions = Nil
    }
  }

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

  @tailrec
  final protected def fire(value: T,
                           previous: Option[T],
                           reactions: List[Reaction[T]]): Unit = if (reactions.nonEmpty) {
    val reaction = reactions.head
    reaction(value, previous)
    fire(value, previous, reactions.tail)
  }
}

object Reactive {
  def fire[T](reactive: Reactive[T], value: T, previous: Option[T]): Unit = {
    reactive.fire(value, previous, reactive.reactions())
  }
}