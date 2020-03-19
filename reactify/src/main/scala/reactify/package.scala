import scala.language.implicitConversions

package object reactify {
  implicit def stateful2Value[T](v: Stateful[T]): T = v()

  /**
    * Syntactic sugar for mutating collections in a `Var`
    */
  implicit class ListVar[T](v: Var[List[T]]) {
    def +=(t: T): Unit = v := (v() ::: List(t))

    def -=(t: T): Unit = v := v().filterNot(_ == t)

    def ++=(seq: Seq[T]): Unit = v := v() ::: seq.toList
  }

  implicit class VectorVar[T](v: Var[Vector[T]]) {
    def +=(t: T): Unit = v := v() :+ t

    def -=(t: T): Unit = v := v().filterNot(_ == t)

    def ++=(seq: Seq[T]): Unit = v := v() ++ seq
  }
}