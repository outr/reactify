package reactify

/**
  * Functionally similar to Var, except represents a far more simplistic and light-weight storage of a value, not a
  * function. Not that Var is heavy, but this is the absolute bare minimum representation of a mutable value while still
  * remaining Observable.
  *
  * @param stored the initial value
  * @param distinct true if this should only fire changes when the new value being set is not equal
  * @tparam T the type of this Prop
  */
class Prop[T](@volatile private var stored: T,
              distinct: Boolean) extends StateChannel[T] {
  override def set(value: => T): Unit = {
    val v: T = value
    if (!distinct || stored != v) {
      stored = v
      fire(v)
    }
  }

  override def observing: Set[Observable[_]] = Set.empty

  override def get: T = stored
}

object Prop {
  /**
    * Creates a new instance of `Prop`.
    */
  def apply[T](value: => T, distinct: Boolean = true): Prop[T] = new Prop[T](value, distinct)
}