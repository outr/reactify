package reactify

import java.util.concurrent.atomic.AtomicBoolean

import reactify.bind.{BindSet, Binding}
import reactify.group.VarGroup

class Var[T] protected() extends Val[T]() with Mutable[T] {
  def this(f: => T) = {
    this()

    set(f)
  }

  override def set(value: => T): Unit = super.set(value)
  override def static(f: T): Unit = super.static(f)

  /**
    * Group multiple Vars together
    */
  def &(that: Var[T]): Var[T] = and(that)

  /**
    * Group multiple Vars together
    */
  def and(that: Var[T]): Var[T] = VarGroup[T](List(this, that))

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
}

object Var {
  def apply[T](f: => T): Var[T] = {
    val v = new Var[T]
    v.set(f)
    v
  }
}