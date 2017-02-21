package reactify

class State[T](private var stateInstance: StateInstance[T]) extends Observable[T] {
  private[reactify] val replacement = new ThreadLocal[Option[StateInstance[T]]] {
    override def initialValue(): Option[StateInstance[T]] = None
  }

  private def instance: StateInstance[T] = replacement.get().getOrElse(stateInstance)

  def observingIds: Set[Int] = instance.observables

  // Listeners to all observables
  Observable.attach((id: Int, _: Any) => if (observingIds.contains(id)) {
    update()
  })

  def get: T = {
    StateInstance.reference(id)
    instance.cached
  }

  def apply(): T = get

  protected def set(value: => T): Unit = synchronized {
    val previous = instance
    stateInstance = StateInstance[T](this, previous, () => value)
  }

  def update(): Unit = {
    val current = instance.cached
    StateInstance.update(instance)
    if (current != instance.cached) {
      fire(instance.cached)
    }
  }
}

class StateInstance[T](val function: () => T) {
  var cached: T = _
  var observables: Set[Int] = Set.empty

  StateInstance.update(this)
}

object StateInstance {
  private val observables = new ThreadLocal[Set[Int]]

  def apply[T](state: State[T], previous: StateInstance[T], function: () => T): StateInstance[T] = {
    var instance = new StateInstance[T](function)
    if (instance.observables.contains(state.id)) {
      instance = new StateInstance[T](() => {
        val original = state.replacement.get()
        state.replacement.set(Some(previous))
        try {
          function()
        } finally {
          state.replacement.set(original)
        }
      })
    }
    instance
  }

  def update[T](instance: StateInstance[T]): Unit = instance.synchronized {
    val previous = observables.get()
    observables.set(Set.empty)
    try {
      instance.cached = instance.function()
      instance.observables = observables.get()
    } finally {
      observables.set(previous)
    }
  }

  def reference(id: Int): Unit = Option(observables.get()) match {
    case Some(obs) => observables.set(obs + id)
    case None => // Nothing being updated
  }
}