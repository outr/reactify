package reactify.standard

import reactify.{Dep, Var}

class StandardDep[T, R](override val name: Option[String],
                        override val owner: Var[R],
                        r2TFunction: R => T,
                        t2RFunction: T => R) extends Dep[T, R] {
  override def t2R(t: T): R = t2RFunction(t)

  override def r2T(r: R): T = r2TFunction(r)
}