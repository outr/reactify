package reactify

class State[T] private() extends Observable[T] {
  private[reactify] var instance: StateInstance[T] = _

  def this(function: () => T) = {
    this()
    StateInstance.replace[T](this, function)
  }

  def observing: Set[Observable[_]] = instance.observables

  def get: T = {
    StateInstance.reference(this)
    instance.value
  }

  def apply(): T = get

  def value: T = get

  def attachAndFire(f: T => Unit): T => Unit = {
    attach(f)
    fire(get)
    f
  }

  override def changes(listener: ChangeListener[T]): (T) => Unit = {
    attach(ChangeListener.createFunction(listener, Some(get)))
  }

  protected def set(value: => T): Unit = synchronized {
    StateInstance.replace[T](this, () => value)
  }

  /**
    * Convenience method to pre-evaluate the value instead of as an anonymous function.
    *
    * @param value the value to be set
    */
  protected def setStatic(value: T): Unit = synchronized {
    val v: T = value
    StateInstance.replace[T](this, () => v)
  }
}

class StateInstance[T](val state: State[T], val function: () => T, val previousInstance: Option[StateInstance[T]]) extends Observable[T] {
  private val replacement = new ThreadLocal[Option[StateInstance[T]]] {
    override def initialValue(): Option[StateInstance[T]] = None
  }
  private var cached: T = _
  var observables: Set[Observable[_]] = Set.empty

  def value: T = replacement.get().map(_.value).getOrElse(cached)

  val monitor: (Any) => Unit = (_: Any) => {
    val original = cached
    update()
    if (original != cached) {
      state.fire(cached)
    }
  }

  update()

  def dispose(): Unit = {
    observables.foreach(_.detach(monitor))
    previousInstance.foreach(_.dispose())
  }

  def update(): Unit = synchronized {
    previousInstance.foreach(_.update())

    val previous = StateInstance.observables.get()
    StateInstance.observables.set(Set.empty)
    try {
      val oldReplacement = replacement.get()
      oldReplacement match {
        case Some(old) => if (old.previousInstance.nonEmpty) {
          replacement.set(old.previousInstance)
        } else {
          replacement.set(oldReplacement)
        }
        case None => replacement.set(previousInstance)
      }
//      replacement.set(oldReplacement.flatMap(_.previousInstance).orElse(previousInstance))
      try {
        cached = function()
      } finally {
        replacement.set(oldReplacement)
      }
      val oldObservables = observables
      val newObservables = StateInstance.observables.get() - state

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

  def replace[T](state: State[T], function: () => T): Unit = {
    val previous = state.instance
    var instance = new StateInstance[T](state, function, None)
    if (instance.observables.contains(state)) {
      instance = new StateInstance[T](state, function, Option(previous))
    } else if (previous != null) {
      previous.dispose()  // Cleanup old instance
    }
    state.instance = instance
    if (previous != null && previous.value != instance.value) {
      state.fire(instance.value)
    }
  }

  def reference(observable: Observable[_]): Unit = Option(observables.get()) match {
    case Some(obs) => observables.set(obs + observable)
    case None => // Nothing being updated
  }
}