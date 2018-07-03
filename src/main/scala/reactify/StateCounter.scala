package reactify

class StateCounter {
  var references: List[State[_]] = Nil
}

object StateCounter {
  private val instance = new ThreadLocal[Option[StateCounter]] {
    override def initialValue(): Option[StateCounter] = None
  }

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

  def referenced(state: State[_]): Unit = {
    instance.get().foreach { counter =>
      counter.references = state :: counter.references
    }
  }
}