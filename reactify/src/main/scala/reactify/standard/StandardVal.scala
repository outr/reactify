package reactify.standard

import reactify.reaction.{Reaction, ReactionStatus, Reactions}
import reactify.{FunctionResult, Reactive, Val, Var}

class StandardVal[T](f: => T, override val name: Option[String]) extends Val[T] {
  private var result: FunctionResult[T] = _
  refresh(f, mode)

  protected def mode: Var.Mode = Var.Mode.Normal

  private lazy val reaction: Reaction[Any] = new Reaction[Any] {
    override def apply(value: Any, previous: Option[Any]): ReactionStatus = {
      recalculate()
      ReactionStatus.Continue
    }
  }

  protected def recalculate(): Unit = refresh(result.function(), result.mode)

  protected def refresh(f: => T, mode: Var.Mode): Unit = synchronized {
    val previousValue = Option(result).map(_.value)
    val previousReferences = Option(result).map(_.referenced).getOrElse(Set.empty)
    mode match {
      case Var.Mode.Normal => result = Reactive.processing(f, mode)
      case Var.Mode.Static => result = new FunctionResult[T](() => f, mode, f, Set.empty)
    }
    val removed = previousReferences.diff(result.referenced)
    val added = result.referenced.diff(previousReferences)
    removed.foreach(_.reactions.asInstanceOf[Reactions[Any]] -= reaction)
    added.foreach(_.reactions.asInstanceOf[Reactions[Any]] += reaction)

    if (!previousValue.contains(result.value)) {
      fire(result.value, previousValue)
    }
  }

  override def get: T = result.value
}