package reactify

case class State[T](owner: Reactive[T], function: () => T) extends Reaction[Any] {
  private var _value: T = _
  private var _references: List[State[_]] = Nil

  override def apply(value: Any): Unit = update()

  def value: T = {
    StateCounter.referenced(this)
    _value
  }

  def references: List[State[_]] = _references

  def update(): Unit = synchronized {
    clearReferences()
    val (value, references) = StateCounter.transaction {
      function()
    }
    val modified = _value != value
    _value = value
    val removed = _references.diff(references)
    val added = references.diff(_references)
    removed.foreach(removeReference)
    added.foreach(addReference)
    _references = references
    if (modified) Reactive.fire(owner, value)
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