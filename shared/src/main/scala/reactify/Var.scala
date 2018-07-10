package reactify

import java.util.concurrent.atomic.AtomicBoolean

import reactify.bind.{BindSet, Binding}
import reactify.group.VarGroup
import reactify.reaction.Reaction
import reactify.standard.StandardVar

trait Var[T] extends Val[T] with Channel[T] {
  def attachAndFire(f: T => Unit, priority: Double = Priority.Normal): Reaction[T] = {
    val reaction = attach(f, priority)
    fire(get, Some(get), reactions())
    reaction
  }

  def and(that: Var[T]): Var[T] = VarGroup[T](None, List(this, that))

  override def map[R](f: T => R): Var[R] = {
    val v = Var[R](f(get))
    attach(v := f(_))
    v
  }

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
  def apply[T](value: => T, name: Option[String] = None): Var[T] = new StandardVar[T](value, name)
}