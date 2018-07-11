package reactify

class StateCounter {
  var references: List[State[_]] = Nil
}

/**
  * StateCounter provides infrastructure to glean the references to `State`s within a functional block of code. This is
  * primarily for internal use, but can be used externally to get additional information regarding references.
  */
object StateCounter {
  private val instance = new ThreadLocal[Option[StateCounter]] {
    override def initialValue(): Option[StateCounter] = None
  }

  /**
    * Use this method to get a list of `State` references used by the underlying function block
    */
  def transaction[Return](f: => Return): (Return, List[State[_]]) = {
    val previous = instance.get()
    val counter = new StateCounter
    instance.set(Some(counter))
    try {
      val value: Return = f
      (value, counter.references)
    } finally {
      instance.set(previous)
    }
  }

  /**
    * Called by a State when it is referenced to get the value
    */
  def referenced(state: State[_]): Unit = {
    instance.get().foreach { counter =>
      counter.references = state :: counter.references
    }
  }
}