package reactify

import reactify.group.ValGroup
import reactify.reaction.{Reaction, ReactionStatus, Reactions}

import scala.util.{Failure, Success, Try}

class Val[T] protected() extends Reactive[T] with Stateful[T] {
  protected def this(f: => T) = {
    this()

    set(f)
  }

  protected var function: () => T = _
  protected var state: Try[T] = _
  protected var previous: Option[T] = None
  protected var _references: Set[Val[_]] = Set.empty

  def references: Set[Val[_]] = _references

  private lazy val reaction: Reaction[Any] = new Reaction[Any] {
    override def apply(value: Any, previous: Option[Any]): ReactionStatus = {
      Val.evaluate(Val.this, updating = true)
      ReactionStatus.Continue
    }
  }

  protected def set(value: => T): Unit = Val.set(this, value)

  def option: Option[T] = state match {
    case Success(t) => Some(t)
    case _ => None
  }
  def toTry: Try[T] = state

  def static(value: T): Unit = {
    function = () => value
    previous = option
    state = Success(value)
    // A static value no longer depends on anything: stop listening
    _references.foreach(_.reactions.asInstanceOf[Reactions[Any]] -= reaction)
    _references = Set.empty
  }

  /**
    * Gets the current value from the current `State`
    */
  override def get: T = Val.get(this)

  /**
    * Group multiple Vals together
    */
  def &(that: Val[T]): Val[T] = and(that)

  /**
    * Group multiple Vals together
    */
  def and(that: Val[T]): Val[T] = ValGroup[T](List(this, that))

  def equality(t1: T, t2: T): Boolean = t1 == t2

  // from the state rather than `get`, which inside an evaluation would make this a dependency of it
  override protected def superseded(value: T): Boolean = option match {
    case Some(current) => !equality(current, value)
    case None => true
  }

  /**
    * Maps this Val to another type.
    */
  def map[R](f: T => R): Val[R] = Val(f(get))

  override def toString: String = s"Var($state)"
}

object Val {
  // null when nothing is being evaluated on this thread (no Option per evaluation and per read)
  private val evaluating: ThreadLocal[Evaluating] = new ThreadLocal[Evaluating] {
    override def initialValue(): Evaluating = null
  }

  def set[T](v: Val[T], value: => T): Unit = setFunction(v, () => value)

  private[reactify] def setFunction[T](v: Val[T], f: () => T): Unit = {
    v.function = f
    evaluate(v, updating = false)
  }

  /** `v` set to a value already in hand, as `@=` sets one: what `evaluate` would come to for a function that reads
    * nothing and cannot throw, without its bookkeeping (no evaluation frame on the thread, no `Try`, no comparing of
    * dependencies). A value that depended on others stops listening to them; an unchanged one fires nothing. Setting a
    * plain value is most of what a UI does, and on a television the bookkeeping was a third of building a screen. */
  private[reactify] def setValue[T](v: Val[T], f: () => T, value: T): Unit = {
    v.function = f
    if (v._references.nonEmpty) {
      v._references.foreach(_.reactions.asInstanceOf[Reactions[Any]] -= v.reaction)
      v._references = Set.empty
    }
    v.state match {
      case Success(p) if value == p => // Same value, ignore
      case _ => {
        v.previous = v.option
        v.state = Success(value)
        Reactive.fire(v, value, v.previous, v.reactions())
      }
    }
  }

  def apply[T](value: => T): Val[T] = {
    val v = new Val[T]
    v.set(value)
    v
  }

  /** Evaluate `v`'s function, and on a new value fire its reactions. Only the function's own exception becomes the
    * value's `Failure` (rethrown by the next read); an exception thrown by a reaction propagates to whoever set the
    * value, rather than marking a value that was evaluated correctly as failed. */
  def evaluate[T](v: Val[T], updating: Boolean): Unit = {
    val original = evaluating.get()
    val e = new Evaluating(v, updating)
    evaluating.set(e)
    val result: Try[T] = try {
      Success(v.function())
    } catch {
      case t: Throwable => Failure(t)
    } finally {
      evaluating.set(original)
    }

    result match {
      case Failure(t) => {
        v.previous = v.option
        v.state = Failure(t)
      }
      case Success(evaluated) => {
        val previousReferences = v._references
        val references = e.references
        // Only when the dependencies changed (never for a value without any, like every `@=`): detach from the
        // dropped ones, attach to the new ones, and remember them.
        if ((previousReferences.nonEmpty || references.nonEmpty) && !sameReferences(previousReferences, references)) {
          val removed = previousReferences.diff(references)
          val added = references.diff(previousReferences)
          removed.foreach(_.reactions.asInstanceOf[Reactions[Any]] -= v.reaction)
          added.foreach(_.reactions.asInstanceOf[Reactions[Any]].append(v.reaction))
          v._references = references
        }
        v.state match {
          case Success(p) if evaluated == p => // Same value, ignore
          case _ => {
            v.previous = v.option
            v.state = Success(evaluated)
            Reactive.fire(v, evaluated, v.previous, v.reactions())
          }
        }
      }
    }
  }

  /** The same dependencies as last time? Checks the common cases — the same set, or one dependency that is the same
    * instance — before full set equality. */
  private def sameReferences(previous: Set[Val[_]], current: Set[Val[_]]): Boolean =
    (previous eq current) ||
      (previous.size == 1 && current.size == 1 && (previous.head eq current.head)) ||
      previous == current

  def get[T](v: Val[T]): T = {
    val e = evaluating.get()
    if (e != null && (e.v eq v) && e.updating) {
      v.previous.getOrElse(throw new RuntimeException("Attempting to get previous on None!"))
    } else {
      if (e != null && !(e.v eq v)) {
        e.references += v
      }
      v.state match {
        case Success(t) => t
        case Failure(t) => throw t
      }
    }
  }

  class Evaluating(val v: Val[_], val updating: Boolean, var references: Set[Val[_]] = Set.empty)
}
