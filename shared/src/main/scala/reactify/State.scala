package reactify

class State[T] private() extends Observable[T] {
  def this(function: () => T) = {
    this()
    stateInstance = new StateInstance[T](this, function)
  }

  private[reactify] var stateInstance: StateInstance[T] = _
  private[reactify] val replacement = new ThreadLocal[Option[StateInstance[T]]] {
    override def initialValue(): Option[StateInstance[T]] = None
  }

  private def instance: StateInstance[T] = replacement.get().getOrElse(stateInstance)

  def observing: Set[Observable[_]] = instance.observables

  def get: T = {
    StateInstance.reference(this)
    instance.cached
  }

  def apply(): T = get

  protected def set(value: => T): Unit = synchronized {
    val previous = instance
    stateInstance = StateInstance[T](previous, () => value)
  }

  def update(): Unit = {
    val current = instance.cached
    instance.update()
    if (current != instance.cached) {
      fire(instance.cached)
    }
  }
}

class StateInstance[T](val state: State[T], val function: () => T) extends Observable[T] {
  var cached: T = _
  var observables: Set[Observable[_]] = Set.empty

  val monitor: (Any) => Unit = (_: Any) => {
    val original = cached
    update()
    if (original != cached) {
      fire(cached)
    }
  }

  update()

  def dispose(): Unit = observables.foreach(_.detach(monitor))

  def update(): Unit = synchronized {
    val previous = StateInstance.observables.get()
    StateInstance.observables.set(Set.empty)
    try {
      cached = function()
      val oldObservables = observables
      val newObservables = StateInstance.observables.get()

      // Out with the old
      oldObservables.foreach { ob =>
        if (!newObservables.contains(ob)) {
          ob.detach(monitor)
        }
      }

      // In with the new
      newObservables.foreach { ob =>
        if (!oldObservables.contains(ob)) {
          ob.attach(monitor)
        }
      }
      observables = newObservables
    } finally {
      StateInstance.observables.set(previous)
    }
  }
}

object StateInstance {
  private val observables = new ThreadLocal[Set[Observable[_]]]

  def apply[T](previous: StateInstance[T], function: () => T): StateInstance[T] = {
    val state = previous.state
    var instance = new StateInstance[T](state, function)
    if (instance.observables.contains(state)) {
      instance.observables += previous
      // TODO: replace state with instance (StateInstance should be an Observable)
      instance = new StateInstance[T](state, () => {
        val original = state.replacement.get()
        state.replacement.set(Some(previous))
        try {
          function()
        } finally {
          state.replacement.set(original)
        }
      })
    } else {
      previous.dispose()      // Not necessary anymore, so dispose the old
    }
    instance
  }

  def reference(observable: Observable[_]): Unit = Option(observables.get()) match {
    case Some(obs) => observables.set(obs + observable)
    case None => // Nothing being updated
  }
}