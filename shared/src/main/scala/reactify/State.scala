package reactify

case class State[T](owner: Reactive[T], index: Long, function: () => T) extends Reaction[Any] {
  private var _previousState: Option[State[T]] = None
  private var _nextState: Option[State[T]] = None
  private var default: T = _
  private var _value: Option[T] = None
  private var _references: List[State[_]] = Nil

  private lazy val updating = new ThreadLocal[Boolean] {
    override def initialValue(): Boolean = false
  }

  override def apply(value: Any, previous: Option[Any]): Unit = {
    println(s"Propagating! ${_previousState}")
    update()
  }

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
      case _ => _value.getOrElse(throw new RuntimeException("State.value has not been set yet!"))
    }
  }

  def references: List[State[_]] = _references

  def update(previous: Option[State[T]] = _previousState): Unit = synchronized {
    new RuntimeException(s"**** UPDATING $owner:$index, previous: ${previous.map(_._value)}").printStackTrace()
    clearReferences()
    _previousState = previous
    val (value, allReferences) = StateCounter.transaction {
      updating.set(true)
      try {
        function()
      } finally {
        updating.set(false)
      }
    }
    val references = allReferences.filterNot(_ == this)
    val previousValue = _value.orElse(previous.map(_.value)).getOrElse(default)
    val modified = previousValue != value
    println(s"Value changed from $previousValue to $value, modified? $modified")
    _value = Some(value)
    val removed = _references.diff(references)
    val added = references.diff(_references)
    removed.foreach(removeReference)
    added.foreach(addReference)
    previous.foreach { previousState =>
      if (allReferences.contains(this)) {
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
        n.update()
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
    _references = Nil
  }

  override def toString: String = s"State(owner: $owner, index: $index, value: ${_value}, active: $active, hasPrevious: ${previousState.nonEmpty}, hasNext: ${nextState.nonEmpty})"
}