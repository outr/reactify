package reactify

case class State[T](owner: Reactive[T], function: () => T) extends Reaction[Any] {
  private var _previousState: Option[State[T]] = None
  private var _nextState: Option[State[T]] = None
  private var _value: T = _
  private var _references: List[State[_]] = Nil

  private lazy val updating = new ThreadLocal[Boolean] {
    override def initialValue(): Boolean = false
  }

  override def apply(value: Any, previous: Option[Any]): Unit = update(previousState)

  def previousState: Option[State[T]] = _previousState

  def nextState: Option[State[T]] = _nextState

  def active: Boolean = nextState.isEmpty

  def value: T = {
    StateCounter.referenced(this)
    previousState match {
      case Some(ps) if updating.get() => {
        val previous = ps.value
        previous
      }
      case _ => _value
    }
  }

  def references: List[State[_]] = _references

  def update(previous: Option[State[T]]): Unit = synchronized {
    clearReferences()
    val (value, references) = StateCounter.transaction {
      updating.set(true)
      try {
        function()
      } finally {
        updating.set(false)
      }
    }
    val previousValue = previous.map(_.value).getOrElse(_value)
    val modified = previousValue != value
    _value = value
    val removed = _references.diff(references)
    val added = references.diff(_references)
    removed.foreach(removeReference)
    added.foreach(addReference)
    previous.foreach { previousState =>
      if (references.contains(this)) {
        this._previousState = Some(previousState)
      } else {
        previousState.clearReferences()
        this._previousState = None
      }
    }
    if (previous.isEmpty) this._previousState = None
    _references = references
    if (modified && active) {
      previous.foreach { p =>
        if (p ne this) {
          p._nextState = Some(this)
        }
      }
      Reactive.fire(owner, value, Some(previousValue))
    } else {
      _nextState.foreach { n =>
        n.update(Some(this))
      }
    }
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

  override def toString: String = s"State(owner: $owner, value: ${_value}, active: $active, previous: $previousState, hasNext: ${nextState.nonEmpty})"
}