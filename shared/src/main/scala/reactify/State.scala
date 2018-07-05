package reactify

case class State[T](owner: Reactive[T], function: () => T) extends Reaction[Any] {
  private var _value: T = _
  private var _references: List[State[_]] = Nil

  override def apply(value: Any, previous: Option[Any]): Unit = update(None)

  def value: T = {
    StateCounter.referenced(this)
    _value
  }

  def references: List[State[_]] = _references

  def update(previous: Option[State[T]]): Unit = synchronized {
    clearReferences()
    val (value, references) = StateCounter.transaction {
      function()
    }
    val previousValue = previous.map(_.value).getOrElse(_value)
    val modified = previousValue != value
    _value = value
    val removed = _references.diff(references)
    val added = references.diff(_references)
    removed.foreach(removeReference)
    added.foreach(addReference)
    _references = references
    if (modified) Reactive.fire(owner, value, Some(previousValue))
  }

  private def addReference(state: State[_]): Unit = {
    state.owner.asInstanceOf[Reactive[Any]].reactions += this
  }

  private def removeReference(state: State[_]): Unit = {
    state.owner.asInstanceOf[Reactive[Any]].reactions -= this
  }

  def clearReferences(): Unit = synchronized {
    references.foreach(removeReference)
  }
}