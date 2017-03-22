package reactify

import scala.concurrent.{Future, Promise}

/**
  * Observable, as the name suggests, observes values being fired against it. This is the core functionality of Reactify
  * and provides the infrastructure used by Channel, Val, Var, Prop, and Dep.
  *
  * @tparam T the type of value this Observable will receive
  */
trait Observable[T] {
  private var observers = Set.empty[T => Unit]

  /**
    * Attaches a function to listen to values fired against this Observable.
    *
    * @param f function listener
    * @return the supplied function. This reference is useful for detaching the function later
    */
  def attach(f: T => Unit): T => Unit = synchronized {
    observers += f
    f
  }

  /**
    * Detaches a function from listening to this Observable.
    *
    * @param f function listener that was previously attached
    */
  def detach(f: T => Unit): Unit = synchronized {
    observers -= f
  }

  /**
    * Invokes the listener only one time and then detaches itself. If supplied, the condition filters the scenarios in
    * which the listener will be invoked.
    *
    * @param f the function listener
    * @param condition the condition under which the listener will be invoked. Defaults to always return true.
    */
  def once(f: T => Unit, condition: T => Boolean = (t: T) => true): Unit = {
    var wrapper: T => Unit = f
    wrapper = attach((t: T) => synchronized {
      if (condition(t)) {
        detach(wrapper)
        f(t)
      }
    })
  }

  /**
    * Returns a Future[T] that represents the value of the next firing of this Observable.
    *
    * @param condition the condition under which the listener will be invoked. Defaults to always return true.
    */
  def future(condition: T => Boolean = (t: T) => true): Future[T] = {
    val promise = Promise[T]
    once(promise.success, condition)
    promise.future
  }

  /**
    * Works similarly to `attach`, but also references the previous value that was fired. This is useful when you need
    * to handle changes, not just new values.
    *
    * @param listener the ChangeListener
    * @return the listener attached. This can be passed to `detach` to remove this listener
    */
  def changes(listener: ChangeListener[T]): T => Unit = attach(ChangeListener.createFunction(listener, None))

  protected[reactify] def fire(value: T): Unit = observers.foreach { obs =>
    obs(value)
  }

  /**
    * Clears all attached observers from this Observable.
    */
  def clear(): Unit = synchronized {
    observers = Set.empty
  }

  /**
    * Cleans up all cross references in preparation for releasing for GC.
    */
  def dispose(): Unit = {
    clear()
  }
}