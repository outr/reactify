package reactify

import reactify.group.ValGroup
import reactify.reaction.{Reaction, ReactionStatus, Reactions}

import scala.util.{Failure, Success, Try}

class Val[T] protected() extends Reactive[T] with Stateful[T] {
  protected def this(f: => T) = {
    this()

    set(f)
  }

  /** What the value is worked out by: null for a value set as it is (`static`, `@=`), which has nothing to work out
    * (`evaluate` and a `Transaction` take the value itself), rather than a function giving it back kept for each. */
  protected var function: () => T = _

  // The state, kept as its parts rather than a `Try` made of them for each value: the value, where `heldFailure` is
  // null; what its function threw, otherwise (its value let go of); `Val.Unset` before it has either.
  private[reactify] var heldValue: T = _
  private[reactify] var heldFailure: AnyRef = Val.Unset

  // The value before its last change, where one is kept (a function reading its own value, evaluated again, reads it):
  // null where none is, as for most values, and `Val.NullPrevious` where it was null, rather than an `Option` of it.
  private[reactify] var heldPrevious: Any = _

  protected var _references: Set[Val[_]] = Set.empty

  /** The value's state: its value, what its function threw, or null before it has either. */
  protected def state: Try[T] = heldFailure match {
    case null => Success(heldValue)
    case f if f eq Val.Unset => null
    case f => Failure(f.asInstanceOf[Throwable])
  }
  protected def state_=(state: Try[T]): Unit = state match {
    case Success(t) =>
      heldValue = t
      heldFailure = null
    case Failure(t) =>
      heldValue = null.asInstanceOf[T]
      heldFailure = t
    case null =>
      heldValue = null.asInstanceOf[T]
      heldFailure = Val.Unset
  }

  /** The value before its last change, where one is kept. */
  protected def previous: Option[T] = heldPrevious match {
    case null => None
    case p => Some(Val.previousValue(p))
  }
  protected def previous_=(previous: Option[T]): Unit = heldPrevious = previous match {
    case Some(p) => Val.asPrevious(p)
    case _ => null
  }

  def references: Set[Val[_]] = _references

  private lazy val reaction: Reaction[Any] = new Reaction[Any] {
    override def apply(value: Any, previous: Option[Any]): ReactionStatus = {
      Val.evaluate(Val.this, updating = true)
      ReactionStatus.Continue
    }
  }

  protected def set(value: => T): Unit = Val.set(this, value)

  def option: Option[T] = heldFailure match {
    case null => Some(heldValue)
    case _ => None
  }
  def toTry: Try[T] = state

  def static(value: T): Unit = {
    function = null
    heldPrevious = Val.currentAsPrevious(this)
    heldValue = value
    heldFailure = null
    // A static value no longer depends on anything: stop listening
    _references.foreach(r => Reactions.remove(r.asInstanceOf[Val[Any]], reaction))
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
  override protected def superseded(value: T): Boolean = heldFailure match {
    case null => !equality(heldValue, value)
    case _ => true
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

  /** What a value with no state yet holds as its failure (`heldFailure`), its state being null: a read of it throws a
    * `MatchError`, made then. Not a `Throwable` itself: one made as the first value is (an app's start) is a stack
    * trace taken there, which on the web, with DevTools open (or Lighthouse, or any client of the DevTools protocol),
    * had the browser work out source positions for the whole script, about 0.3 s on a phone before the page showed. */
  private[reactify] val Unset: AnyRef = new Reactive.Marker

  /** What a value whose value before its last change was null holds as that (`heldPrevious`), null being none. */
  private[reactify] val NullPrevious: AnyRef = new Reactive.Marker

  /** `p`, as `heldPrevious` holds it. */
  private[reactify] def asPrevious(p: Any): Any = if (p == null) NullPrevious else p

  /** What `heldPrevious` holds, as the value it was. */
  private[reactify] def previousValue[T](held: Any): T = {
    if (held.asInstanceOf[AnyRef] eq NullPrevious) null.asInstanceOf[T] else held.asInstanceOf[T]
  }

  /** `v`'s value as its previous one, as `heldPrevious` holds it, or null where it has none (`v.option` was None). */
  private[reactify] def currentAsPrevious(v: Val[_]): Any = v.heldFailure match {
    case null => asPrevious(v.heldValue)
    case _ => null
  }

  /** What `v` is worked out by, for a `Transaction` to put back: its function, or for a value set as it is, one giving
    * that value. */
  private[reactify] def functionOf[T](v: Val[T]): () => T = {
    val f = v.function
    if (f != null) f
    else v.heldFailure match {
      case null =>
        val value = v.heldValue
        () => value
      case _ => null
    }
  }

  def set[T](v: Val[T], value: => T): Unit = setFunction(v, () => value)

  private[reactify] def setFunction[T](v: Val[T], f: () => T): Unit = {
    v.function = f
    evaluate(v, updating = false)
  }

  /** `v` set to a value already in hand, as `@=` sets one: what `evaluate` would come to for a function that reads
    * nothing and cannot throw, without its bookkeeping (no evaluation frame on the thread, no `Try`, no comparing of
    * dependencies), and with no function kept for it. A value that depended on others stops listening to them; an
    * unchanged one fires nothing. Setting a plain value is most of what a UI does, and on a television the bookkeeping
    * was a third of building a screen. */
  private[reactify] def setValue[T](v: Val[T], value: T): Unit = {
    v.function = null
    if (v._references.nonEmpty) {
      v._references.foreach(r => Reactions.remove(r.asInstanceOf[Val[Any]], v.reaction))
      v._references = Set.empty
    }
    v.heldFailure match {
      case null if value == v.heldValue => // Same value, ignore
      case _ => {
        val previous = v.option
        v.heldPrevious = currentAsPrevious(v)
        v.heldValue = value
        v.heldFailure = null
        v.fireHeard(value, previous)
        // heard: a value set plainly has no more use for the one before it (only a function that reads its own value does,
        // and the evaluation that makes one sets it again), and kept, it held what it was (a screen left, a component taken
        // apart) until the next change
        v.heldPrevious = null
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
    * value, rather than marking a value that was evaluated correctly as failed. A value set as it is (no function) is
    * evaluated as its function was, reading nothing: to the value it has. */
  def evaluate[T](v: Val[T], updating: Boolean): Unit = {
    val f = v.function
    var evaluated: T = null.asInstanceOf[T]
    var failed: Throwable = null
    var references: Set[Val[_]] = Set.empty
    if (f == null && (v.heldFailure match { case null => true; case _ => false })) {
      evaluated = v.heldValue
    } else {
      val original = evaluating.get()
      val e = new Evaluating(v, updating)
      evaluating.set(e)
      try {
        evaluated = f()
      } catch {
        case t: Throwable => failed = t
      } finally {
        evaluating.set(original)
      }
      references = e.references
    }

    failed match {
      case null => {
        val previousReferences = v._references
        // Only when the dependencies changed (never for a value without any, like every `@=`): detach from the
        // dropped ones, attach to the new ones, and remember them.
        if ((previousReferences.nonEmpty || references.nonEmpty) && !sameReferences(previousReferences, references)) {
          val removed = previousReferences.diff(references)
          val added = references.diff(previousReferences)
          removed.foreach(r => Reactions.remove(r.asInstanceOf[Val[Any]], v.reaction))
          added.foreach(r => Reactions.append(r.asInstanceOf[Val[Any]], v.reaction))
          v._references = references
        }
        v.heldFailure match {
          case null if evaluated == v.heldValue => // Same value, ignore
          case _ => {
            val previous = v.option
            v.heldPrevious = currentAsPrevious(v)
            v.heldValue = evaluated
            v.heldFailure = null
            v.fireHeard(evaluated, previous)
          }
        }
      }
      case t => {
        v.heldPrevious = currentAsPrevious(v)
        v.heldValue = null.asInstanceOf[T]
        v.heldFailure = t
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
    // a function reading its own value, evaluated again for a change to what it reads, reads the value before its own last
    // change; one with none kept (the value was last set plainly, or never changed) reads what it is
    if (e != null && (e.v eq v) && e.updating && v.heldPrevious != null) {
      previousValue[T](v.heldPrevious)
    } else {
      if (e != null && !(e.v eq v)) {
        e.references += v
      }
      v.heldFailure match {
        case null => v.heldValue
        case u if u eq Unset => throw new MatchError(null)
        case t => throw t.asInstanceOf[Throwable]
      }
    }
  }

  class Evaluating(val v: Val[_], val updating: Boolean, var references: Set[Val[_]] = Set.empty)
}
