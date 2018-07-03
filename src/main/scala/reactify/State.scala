package reactify

case class State[T](owner: Reactive[T], function: () => T) {
  private var _value: T = _
  private var _references: List[State[_]] = Nil

  private lazy val reaction: Reaction[Any] = new Reaction[Any] {
    override def apply(value: Any): Unit = update()
  }

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
    _value = value
    _references = references
  }

  private def clearReferences(): Unit = {
    references.foreach(_.owner.asInstanceOf[Reactive[Any]].reactions -= reaction)
  }
}