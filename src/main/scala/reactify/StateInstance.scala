package reactify

class StateInstance[T](val state: State[T], val function: () => T, val previousInstance: Option[StateInstance[T]]) extends Observable[T] {
  private val replacement = new ThreadLocal[Option[StateInstance[T]]] {
    override def initialValue(): Option[StateInstance[T]] = None
  }
  private var cached: T = _
  var observables: Set[Observable[_]] = Set.empty
  var hasSelfReference: Boolean = false

  def value: T = replacement.get().map(_.value).getOrElse(cached)

  val monitor: (Any) => Unit = (_: Any) => {
    val original = cached
    state.instance.update()
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
      try {
        cached = function()
      } finally {
        replacement.set(oldReplacement)
      }
      val oldObservables = observables
      hasSelfReference = StateInstance.observables.get().contains(state)
      var newObservables = StateInstance.observables.get()
      if (hasSelfReference) {
        newObservables -= state
      }
      previousInstance match {
        case Some(pi) => newObservables += pi
        case None => // Nothing to do
      }

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

  def reference(observable: Observable[_]): Unit = Option(observables.get()) match {
    case Some(obs) => observables.set(obs + observable)
    case None => // Nothing being updated
  }
}