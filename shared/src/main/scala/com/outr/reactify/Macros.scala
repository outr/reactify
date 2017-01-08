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

    observables.foreach { o =>
      if (o.equalsStructure(channel)) {
        c.abort(c.enclosingPosition, s"Recursive reference detected ($channel) in assignment (including self in the assignment function). $value")
      }
    }
    q"""
        val previousValue = $channel.get
        $channel.update(List(..$observables), $value)
     """
  }

  def mod(c: blackbox.Context)(f: c.Tree): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(f)
    val channel = c.prefix.tree
    q"""
        val previousValue = $channel.get
        $channel.update(List(..$observables), $f(previousValue))
     """
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
