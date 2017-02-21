package reactify

class State[T](private var instance: StateInstance[T]) extends Observable[T] {
  private var previous: StateInstance[T] = _

  def observingIds: Set[Int] = instance.observables

  // Listeners to all observables
  Observable.attach((id: Int, _: Any) => if (observingIds.contains(id)) {
    update()
  })

  def get: T = {
    StateInstance.reference(id)
    instance.cached
  }

  protected def set(value: => T): Unit = synchronized {
    previous = instance
    instance = new StateInstance[T](() => value)
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
  private val observing = new ThreadLocal[Var[_]]
  private val observables = new ThreadLocal[Set[Int]]

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