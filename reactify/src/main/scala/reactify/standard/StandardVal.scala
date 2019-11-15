package reactify.standard

import reactify.reaction.{Reaction, ReactionStatus, Reactions}
import reactify.{FunctionResult, Reactive, Val, Var}

class StandardVal[T](f: => T, override val name: Option[String]) extends Val[T] {
  private var result: FunctionResult[T] = _
  refresh(f, mode, replacing = true)

  protected def mode: Var.Mode = Var.Mode.Normal

  private lazy val reaction: Reaction[Any] = new Reaction[Any] {
    override def apply(value: Any, previous: Option[Any]): ReactionStatus = {
      println(s"reaction recalculate: $value, previous: $previous")
      recalculate()
      ReactionStatus.Continue
    }
  }

  protected def recalculate(): Unit = {
    println("Recalculating!")
    refresh(result.function(), result.mode, replacing = false)
  }

  protected def refresh(f: => T, mode: Var.Mode, replacing: Boolean): Unit = synchronized {
    val previousValue = Option(result).map(_.value)
    val previousReferences = Option(result).map(_.referenced).getOrElse(Set.empty)
    val previous = if (replacing) {
      previousValue.getOrElse(null.asInstanceOf[T])
    } else {
      Option(result).map(_.previous).getOrElse(null.asInstanceOf[T])
    }
    mode match {
      case Var.Mode.Normal => result = Reactive.processing(this, f, mode, previous)
      case Var.Mode.Static => result = new FunctionResult[T](() => f, mode, f, previous, Set.empty)
    }
    val removed = previousReferences.diff(result.referenced)
    val added = result.referenced.diff(previousReferences)
    removed.foreach(_.reactions.asInstanceOf[Reactions[Any]] -= reaction)
    added.foreach(_.reactions.asInstanceOf[Reactions[Any]] += reaction)

    if (!previousValue.contains(result.value)) {
      fire(result.value, previousValue)
    }
  }

  override def get: T = Reactive.getting(this).getOrElse(result.value)
}