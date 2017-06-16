package reactify

import reactify.instance.RecursionMode

/**
  * Var, as the name suggests, is very similar to a Scala `var`. The value is defined during instantiation, but may be
  * modified later. The value may be a static value, or may be a derived value depending on multiple `Observables`. If
  * `Observables` make up the value they will be monitored and events fired on this `Observable` as the value changes.
  *
  * @tparam T the type of value this channel receives
  */
class Var[T](function: () => T,
             distinct: Boolean = true,
             cache: Boolean = true,
             recursion: RecursionMode = RecursionMode.RetainPreviousValue,
             transactional: Boolean = true,
             onUpdate: Boolean = false
            ) extends Val[T](function, distinct, cache, recursion, transactional, onUpdate) with StateChannel[T] {
  override def set(value: => T): Unit = super.set(value)
  def asVal: Val[T] = this

  override def toString: String = s"Var($get)"
}

object Var {
  /**
    * Creates a new instance of `Var`.
    */
  def apply[T](value: => T,
               static: Boolean = false,
               distinct: Boolean = true,
               cache: Boolean = true,
               recursion: RecursionMode = RecursionMode.RetainPreviousValue,
               transactional: Boolean = true): Var[T] = {
    val f = if (static) {
      val v: T = value
      () => v
    } else {
      () => value
    }
    new Var[T](f, distinct, cache, recursion, transactional)
  }

  /**
    * Creates a simple Var instance that only stores the value from the function, not the function.
    */
  def prop[T](value: => T, transactional: Boolean = true): Var[T] = {
    apply(value, static = true, recursion = RecursionMode.Static, transactional = transactional)
  }

  /**
    * Creates a new instance of `Var` mixing in `DirtyState`.
    */
  def dirty[T](value: => T,
               static: Boolean = false,
               distinct: Boolean = true,
               cache: Boolean = true,
               recursion: RecursionMode = RecursionMode.RetainPreviousValue,
               transactional: Boolean = true): Var[T] with DirtyState[T] = {
    val f = if (static) {
      val v: T = value
      () => v
    } else {
      () => value
    }
    new Var[T](f, distinct, cache, recursion, transactional, onUpdate = true) with DirtyState[T]
  }

  def bound[T](get: => T,
               set: T => Unit,
               setImmediately: Boolean = false,
               static: Boolean = false,
               distinct: Boolean = true,
               cache: Boolean = true,
               recursion: RecursionMode = RecursionMode.RetainPreviousValue,
               transactional: Boolean = true): Var[T] = {
    val v = Var[T](get, static, distinct, cache, recursion, transactional)
    if (setImmediately) {
      set(v())
    }
    v.attach(t => set(t))
    v
  }
}