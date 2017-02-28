package reactify

import java.util.concurrent.atomic.AtomicReference

abstract class State[T] private(distinct: Boolean, cache: Boolean) extends Observable[T] {
  private var lastValue: T = _
  private val function = new AtomicReference[() => T]
  private val previous = new AtomicReference[Option[PreviousFunction[T]]](None)
  private val monitoring = new AtomicReference[Set[Observable[_]]](Set.empty)

  private val replacement = new ThreadLocal[Option[PreviousFunction[T]]] {
    override def initialValue(): Option[PreviousFunction[T]] = None
  }

  private val monitor: (Any) => Unit = (_: Any) => {
    updateValue(get(cache = false))
  }

  private def updateValue(value: T): Unit = {
    if (!distinct || value != lastValue) {
      lastValue = value
      fire(value)
    }
  }

  def this(function: () => T,
           distinct: Boolean = true,
           cache: Boolean = true) = {
    this(distinct, cache)
    replace(function)
  }

  def observing: Set[Observable[_]] = monitoring.get()

  def get: T = get(cache)

  def get(cache: Boolean): T = replacement.get() match {
    case Some(p) => {
      replacement.set(p.previous)
      p.function()
    }
    case None => {
      State.reference(this)
      replacement.set(previous.get())
      try {
        if (cache) {
          lastValue
        } else {
          function.get()()
        }
      } finally {
        replacement.set(None)
      }
    }
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
    previous.set(Some(new PreviousFunction[T](this.function.get(), previous.get())))
    val previousObservables = State.observables.get()
    State.observables.set(Set.empty)
    try {
      this.function.set(function)
      val value: T = get(cache = false)

      val oldObservables = observing
      var newObservables = State.observables.get()
      if (!newObservables.contains(this)) {
        // No recursive reference, we can clear previous
        previous.set(None)
      }
      newObservables -= this
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

      monitoring.set(newObservables)
      updateValue(value)
    } finally {
      State.observables.set(previousObservables)
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

object State {
  private val observables = new ThreadLocal[Set[Observable[_]]]

  def reference(observable: Observable[_]): Unit = Option(observables.get()) match {
    case Some(obs) => observables.set(obs + observable)
    case None => // Nothing being updated
  }
}

class PreviousFunction[T](val function: () => T,
                          val previous: Option[PreviousFunction[T]])