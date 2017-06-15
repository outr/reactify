import scala.language.implicitConversions

package object reactify {
  /**
    * Converts a `State[T]` to `T` implicitly. This is useful for DSL type-based operations like `5 + stateVar`.
    */
  implicit def state2Value[T](p: State[T]): T = p()
  implicit def function2Listener[T](f: T => Unit): Listener[T] = Listener[T](f)

  /**
    * Syntactic sugar for mutating collections in a `StateChannel`
    */
  implicit class ListStateChannel[T](v: StateChannel[List[T]]) {
    def +=(t: T): Unit = v := (v() ::: List(t))

    def -=(t: T): Unit = v := v().filterNot(_ == t)

    def ++=(seq: Seq[T]): Unit = v := v() ::: seq.toList
  }

  implicit class VectorStateChannel[T](v: StateChannel[Vector[T]]) {
    def +=(t: T): Unit = v := v() :+ t

    def -=(t: T): Unit = v := v().filterNot(_ == t)

    def ++=(seq: Seq[T]): Unit = v := v() ++ seq
  }
}