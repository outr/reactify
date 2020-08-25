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

  def option: Option[T] = Option(state).flatMap(_.toOption)
  def toTry: Try[T] = state

  def static(value: T): Unit = {
    function = () => value
    previous = option
    state = Success(value)
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

  override def toString: String = s"Var($state)"
}

object Val {
  private val evaluating: ThreadLocal[Option[Evaluating]] = new ThreadLocal[Option[Evaluating]] {
    override def initialValue(): Option[Evaluating] = None
  }

  def set[T](v: Val[T], value: => T): Unit = {
    v.function = () => value
    evaluate(v, updating = false)
  }

  def apply[T](value: => T): Val[T] = {
    val v = new Val[T]
    v.set(value)
    v
  }

  def evaluate[T](v: Val[T], updating: Boolean): Unit = try {
    val original = evaluating.get()
    val e = new Evaluating(v, updating)
    evaluating.set(Some(e))
    val evaluated = try {
      v.function()
    } finally {
      evaluating.set(original)
    }

    val previousReferences = v._references
    val removed = previousReferences.diff(e.references)
    val added = e.references.diff(previousReferences)
    removed.foreach(_.reactions.asInstanceOf[Reactions[Any]] -= v.reaction)
    added.foreach(_.reactions.asInstanceOf[Reactions[Any]] += v.reaction)
    v.state match {
      case Success(p) if evaluated == p => // Same value, ignore
      case s => {
        v.previous = v.option
        v.state = Success(evaluated)
        Reactive.fire(v, evaluated, v.previous, v.reactions())
      }
    }
  } catch {
    case t: Throwable => {
      v.previous = v.option
      v.state = Failure(t)
    }
  }

  def get[T](v: Val[T]): T = {
    evaluating.get() match {
      case Some(e) if e.v eq v => {
        if (e.updating) {
          v.previous.getOrElse(throw new RuntimeException("Attempting to get previous on None!"))
        } else {
          v.state match {
            case Success(t) => t
            case Failure(t) => throw t
          }
        }
      }
      case Some(e) => {
        e.references += v

        v.state match {
          case Success(t) => t
          case Failure(t) => throw t
        }
      }
      case None => v.state match {
        case Success(t) => t
        case Failure(t) => throw t
      }
    }
  }

  class Evaluating(val v: Val[_], val updating: Boolean, var references: Set[Val[_]] = Set.empty)
}