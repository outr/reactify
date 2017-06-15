package reactify

import java.util.concurrent.atomic.AtomicBoolean

import reactify.bind.{BindSet, Binding}

import scala.concurrent.Future

/**
  * Dep is very much like a `Val`, but is also a `Channel`. The basic purpose is to represent a value dependent upon
  * another variable. An example of this might be if you are representing a position `left` and you also wanted to
  * represent `right` as well (which would be `left` + `width`). These two variables are dependent upon each other and
  * don't fit well as `Var`s. An example usage might be:
  *
  * <code>
  *   val left: Var[Double] = Var(0.0)
  *   val width: Var[Double] = Var(0.0)
  *   val right: Dep[Double, Double] = Dep(left, width)
  * </code>
  *
  * If an instance is `submissive` it removes `adjustment` from being part of the mutation dependency. For example: in
  * the above scenario if you set `width` to 100.0 and `right` to 125.0 then `left` will be 25.0. Now, what should happen
  * if you change `width` to 50.0? Should `left` change to 75.0 (`submissive = false`) or should `right` change to 75.0
  * (`submissive = true`)?
  */
//class Dep[T, V](variable: Var[V],
//                adjustment: => T,
//                submissive: Boolean)
//               (implicit connector: DepConnector[T, V]) extends StateChannel[T] {
//  override def distinct: Boolean = true
//
//  override protected def value(): T = internal.get
//
//  private val internal = Val[T](connector.combine(variable, adjustment))
//
//  override def attach(f: (T) => Unit, priority: Double = Listener.Priority.Normal): Listener[T] = {
//    internal.attach(f, priority)
//  }
//
//  override def detach(listener: Listener[T]): Unit = internal.detach(listener)
//
//  override def changes(listener: ChangeListener[T]): Listener[T] = internal.changes(listener)
//
//  override protected[reactify] def fire(value: T): Unit = {}
//
//  override def set(value: => T): Unit = set(value, submissive)
//
//  override def observing: Set[Observable[_]] = internal.observing
//
//  def set(value: => T, submissive: Boolean): Unit = {
//    if (submissive) {
//      val adj: T = adjustment
//      variable := connector.extract(value, adj)
//    } else {
//      variable := connector.extract(value, adjustment)
//    }
//  }
//
//  override def apply(): T = internal.apply()
//
//  override def toString: String = s"Dep($get)"
//}

object Dep {
  /**
    * Creates a new `Dep` instance for the referenced channel.
    *
    * @param variable the variable depended on
    * @param adjustment the adjustment to derive the value of the dependency from the variable
    * @param submissive whether this Dep should submit to the variable.
    *
    * @return dependency instance
    */
//  def apply[T, V](variable: Var[V],
//                  adjustment: => T,
//                  submissive: Boolean = false)
//                 (implicit connector: DepConnector[T, V]): Dep[T, V] = {
//    new Dep[T, V](variable, adjustment, submissive)
//  }

  def apply[T, V](variable: Var[V],
                  adjustment: => T,
                  submissive: Boolean = false)
                 (implicit connector: DepConnector[T, V]): Dep[T, V] = {
//    val v = Var[T](connector.combine(variable, adjustment))
//    v.bind(variable, BindSet.None)(
//      t2v = (t: T) => connector.extract(t, adjustment),
//      v2t = (v: V) => connector.combine(v, adjustment)
//    )
//    v
    new Dep[T, V](variable, adjustment, submissive)
  }
}

class Dep[T, V](variable: Var[V],
                adjustment: => T,
                submissive: Boolean)
               (implicit connector: DepConnector[T, V]) extends Var[T](() => connector.combine(variable, adjustment)) {
  assert(!submissive, "Submissive is currently disabled until it can be more thoroughly tested.")

  override def set(value: => T): Unit = set(value, submissive)

  private lazy val changing = new AtomicBoolean(false)

  def set(value: => T, submissive: Boolean): Unit = if (changing.compareAndSet(false, true)) {
    try {
      super.set(value)
      if (submissive) {
        val adj: T = adjustment
        variable := connector.extract(get, adj)
      } else {
        variable := connector.extract(get, adjustment)
      }
    } finally {
      changing.set(false)
    }
  }

  variable.attach { v =>
    if (changing.compareAndSet(false, true)) {
      try {
        super.set(connector.combine(variable, adjustment))
      } finally {
        changing.set(false)
      }
    }
  }
}

/*
class Dep[T, V](variable: Var[V],
                adjustment: => T,
                submissive: Boolean)
               (implicit connector: DepConnector[T, V]) extends Val[T](() => connector.combine(variable, adjustment)) with StateChannel[T] {
  override def set(value: => T): Unit = {
    if (submissive) {
      val adj: T = adjustment
      variable := connector.extract(get, adj)
    } else {
      variable := connector.extract(get, adjustment)
    }
  }
}*/
