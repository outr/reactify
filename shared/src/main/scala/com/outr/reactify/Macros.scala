package com.outr.reactify

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable Macros for expansion")
object Macros {
  private def retrieveObservables(c: blackbox.Context)(value: c.Tree): List[c.Tree] = {
    import c.universe._

    implicit val observableTypeTag = typeTag[Observable[_]]
    val observables = value.collect {
      case tree if tree.tpe <:< c.typeOf[Observable[_]] => tree
    }
    observables
  }

  def set(c: blackbox.Context)(value: c.Tree): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(value)
    val channel = c.prefix.tree
    q"$channel.update(List(..$observables), $value)"
  }

  def newVar[T](c: blackbox.Context)(value: c.Tree)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(value)
    q"new Var[$t](List(..$observables), $value)"
  }

  def newVal[T](c: blackbox.Context)(value: c.Tree)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(value)
    q"new Val[$t](List(..$observables), $value)"
  }
}
