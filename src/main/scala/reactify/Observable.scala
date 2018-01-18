package reactify

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Observable, as the name suggests, observes values being fired against it. This is the core functionality of Reactify
  * and provides the infrastructure used by Channel, Val, Var, Prop, and Dep.
  *
  * @tparam T the type of value this Observable will receive
  */
trait Observable[T] {
  private[reactify] var _observers = List.empty[Observer[T]]

  /**
    * List of all the observers currently observing changes to this Observable.
    */
  def observers: List[Observer[T]] = _observers

  /**
    * Attaches a function to observe values fired against this Observable.
    *
    * @param f function observer
    * @return the supplied function. This reference is useful for detaching the function later
    */
  def attach(f: T => Unit,
             priority: Double = Observer.Priority.Normal): Observer[T] = {
    observe(Observer[T](f, priority))
  }

  /**
    * Direct attachment of a observer.
    *
    * @param observer the observer to attach
    * @return the same observer supplied
    */
  def observe(observer: Observer[T]): Observer[T] = synchronized {
    _observers = (_observers ::: List(observer)).sorted
    observer
  }

  /**
    * Works like `attach`, but doesn't receive the fired value.
    *
    * @param f function to invoke on fire
    * @return observer
    */
  def on(f: => Unit, priority: Double = Observer.Priority.Normal): Observer[T] = attach(_ => f, priority)

  /**
    * Detaches a function from observing this Observable.
    *
    * @param observer function observer that was previously attached
    */
  def detach(observer: Observer[T]): Unit = synchronized {
    _observers = _observers.filterNot(_ eq observer)
  }

  /**
    * Invokes the observer only one time and then detaches itself. If supplied, the condition filters the scenarios in
    * which the observer will be invoked.
    *
    * @param f the function observer
    * @param condition the condition under which the observer will be invoked. Defaults to always return true.
    */
  def once(f: T => Unit,
           condition: T => Boolean = (_: T) => true,
           priority: Double = Observer.Priority.Normal): Observer[T] = {
    var observer: Observer[T] = null
    observer = Observer[T]((value: T) => if (condition(value)) {
      detach(observer)
      f(value)
    }, priority)
    observe(observer)
  }

  /**
    * Returns a Future[T] that represents the value of the next firing of this Observable.
    *
    * @param condition the condition under which the observer will be invoked. Defaults to always return true.
    */
  def future(condition: T => Boolean = (_: T) => true): Future[T] = {
    val promise = Promise[T]
    once(promise.success, condition)
    promise.future
  }

  /**
    * Works similarly to `attach`, but also references the previous value that was fired. This is useful when you need
    * to handle changes, not just new values.
    *
    * @param observer the ChangeObserver
    * @return the observer attached. This can be passed to `detach` to remove this observer
    */
  def changes(observer: ChangeObserver[T]): Observer[T] = attach(ChangeObserver.createFunction(observer, None))

  /**
    * Maps the Observable to another type.
    *
    * @param f function to handle the mapping from T to R
    * @tparam R the type of the new Observable
    * @return Observable[R]
    */
  def map[R](f: T => R): Observable[R] = {
    val channel = Channel[R]
    attach(t => channel := f(t))
    channel
  }

  def collect[R](f: PartialFunction[T, R]): Observable[R] = {
    val channel = Channel[R]
    val lifted = f.lift
    attach { t =>
      lifted(t).foreach(v => channel.set(v))
    }
    channel
  }

  protected[reactify] def fire(value: T, `type`: InvocationType): Unit = Invocation().wrap {
    fireRecursive(value, `type`, Invocation(), _observers)
  }

  final protected def fireRecursive(value: T, `type`: InvocationType, invocation: Invocation, observers: List[Observer[T]]): Unit = {
    if (!invocation.isStopped) {
      observers.headOption.foreach { observer =>
        observer(value, `type`)
        fireRecursive(value, `type`, invocation, observers.tail)
      }
    }
  }

  /**
    * Clears all attached observers from this Observable.
    */
  def clearObservers(): Unit = synchronized {
    _observers = List.empty
  }

  /**
    * Cleans up all cross references in preparation for releasing for GC.
    */
  def dispose(): Unit = {
    clearObservers()
  }

  def and(that: Observable[T]): Observable[T] = Observable.wrap(this, that)
}

object Observable {
  def wrap[T](observables: Observable[T]*): Observable[T] = new WrappedObservable[T](observables.toList)
  def apply[T](init: (T => Unit) => Unit): Observable[T] = new Observable[T] {
    init(fire(_, InvocationType.Direct))
  }
  def apply[T](future: Future[T]): Observable[T] = apply(fire => future.foreach(fire))
}