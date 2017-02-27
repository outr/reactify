package reactify

class State[T] private[reactify]() extends Observable[T] {
  private[reactify] var instance: StateInstance[T] = _

  def this(function: () => T) = {
    this()
    replace(function)
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
    replace(() => value)
  }

  protected def replace(function: () => T): Unit = {
    val previous = this.instance
    var instance = new StateInstance[T](this, function, None)
    if (instance.hasSelfReference) {
      instance = new StateInstance[T](this, function, Option(previous))
    } else if (previous != null) {
      previous.dispose()  // Cleanup old instance
    }
    this.instance = instance
    if (previous != null && previous.value != instance.value) {
      fire(instance.value)
    }
  }

  /**
    * Convenience method to pre-evaluate the value instead of as an anonymous function.
    *
    * @param value the value to be set
    */
  protected def setStatic(value: T): Unit = synchronized {
    val v: T = value
    replace(() => v)
  }
}