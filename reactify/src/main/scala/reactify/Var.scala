package reactify

import java.util.concurrent.atomic.AtomicBoolean

import reactify.bind.{BindSet, Binding}
import reactify.group.VarGroup
import reactify.standard.StandardVar

/**
  * Var represents the combination of `Val` and `Channel` into a stateful and mutable underlying value.
  *
  * @tparam T the type of value this Reactive receives
  */
trait Var[T] extends Val[T] with Channel[T] {
  /**
    * Operating mode of this Var. Defaults to `Normal`
    */
  def mode: Var.Mode

  /**
    * Statically sets a value without monitoring effects
    *
    * @param value the value to assign
    */
  def static(value: T): Unit = set(value, Var.Mode.Static)

  override def @=(value: T): Unit = set(value, Var.Mode.Static)

  /**
    * Group multiple Vars together
    */
  def &(that: Var[T]): Var[T] = and(that)

  def set(value: => T, mode: Var.Mode): Unit

  /**
    * Group multiple Vars together
    */
  def and(that: Var[T]): Var[T] = VarGroup[T](None, List(this, that))

  /**
    * Functional mapping of this Var into another Var.
    *
    * @param f conversion function
    * @tparam R the type of the new Var
    * @return Var[R]
    */
  override def map[R](f: T => R): Var[R] = {
    val v = Var[R](f(get))
    attach(v := f(_))
    v
  }

  /**
    * Convenience method to create a binding between two `Var`s
    *
    * @param that the second `Var` to bind between
    * @param setNow the `BindSet` value (Defaults to LeftToRight)
    * @param t2v implicit function conversion from T to V
    * @param v2t implicit function conversion from V to T
    * @tparam V the type of the second `Var`
    * @return Binding[T, V]
    */
  def bind[V](that: Var[V], setNow: BindSet = BindSet.LeftToRight)
             (implicit t2v: T => V, v2t: V => T): Binding[T, V] = {
    setNow match {
      case BindSet.LeftToRight => that := t2v(this)
      case BindSet.RightToLeft => this := v2t(that)
      case BindSet.None => // Nothing
    }
    val changing = new AtomicBoolean(false)
    val leftToRight = this.attach { t =>
      if (changing.compareAndSet(false, true)) {
        try {
          that := t2v(get)
        } finally {
          changing.set(false)
        }
      }
    }
    val rightToLeft = that.attach { t =>
      if (changing.compareAndSet(false, true)) {
        try {
          set(v2t(that.get))
        } finally {
          changing.set(false)
        }
      }
    }
    new Binding(this, that, leftToRight, rightToLeft)
  }

  override def toString: String = name.getOrElse("Var")
}

object Var {
  def apply[T](value: => T,
               mode: Mode = Mode.Normal,
               name: Option[String] = None): Var[T] = new StandardVar[T](value, mode, name)

  sealed trait Mode

  object Mode {
    case object Normal extends Mode
    case object Static extends Mode
  }
}