package com.outr.reactify

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable Macros for expansion")
object Macros {
  private def retrieveObservables(c: blackbox.Context)(value: c.Tree): List[c.Tree] = {
    import c.universe._

    implicit val observableTypeTag = typeTag[Observable[_]]
    val observables = value.collect {
      case tree if tree.tpe <:< c.typeOf[Observable[_]] && !(tree.tpe =:= c.typeOf[Nothing]) => tree
    }
    observables
  }

  def set(c: blackbox.Context)(value: c.Tree): c.Tree = {
    import c.universe._

    def setChannel(value: c.Tree): c.Tree = {
      val observables = retrieveObservables(c)(value)
      val channel = c.prefix.tree

      q"$channel.update(List(..$observables), $value)"
    }

    def setStateChannel(value: c.Tree): c.Tree = {
      val observables = retrieveObservables(c)(value)
      val channel = c.prefix.tree
      val selfReference = observables.exists(_.equalsStructure(channel))

      val untyped =
        q"""
            val previousValue = _root_.com.outr.reactify.State.internalFunction($channel)
            val previousVal = _root_.com.outr.reactify.Val(previousValue())
         """
      val retyped = c.typecheck(untyped)
      val transformed = if (selfReference) {
        val transformer = new Transformer {
          override def transform(tree: c.universe.Tree): c.universe.Tree = if (tree.equalsStructure(channel)) {
            val t = q"previousVal"
            val Block(_ :: v :: Nil, _) = retyped
            c.internal.setSymbol(t, v.symbol)
            c.internal.setType(t, v.tpe)
          } else {
            super.transform(tree)
          }
        }
        transformer.transform(value)
      } else {
        value
      }
      val res = q"$channel.update(List(..$observables), $transformed)"
      q"$retyped ; $res"
    }

    if (c.prefix.tree.tpe <:< c.typeOf[StateChannel[_]]) {
      setStateChannel(value)
    } else {
      setChannel(value)
    }
  }

  def newVar[T](c: blackbox.Context)(value: c.Tree)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(value)
    q"new _root_.com.outr.reactify.Var[$t](List(..$observables), $value)"
  }

  def newVal[T](c: blackbox.Context)(value: c.Tree)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(value)
    q"new _root_.com.outr.reactify.Val[$t](List(..$observables), $value)"
  }

  def newDep[T, V](c: blackbox.Context)
                  (variable: c.Tree, adjustment: c.Tree)
                  (connector: c.Tree)
                  (implicit t: c.WeakTypeTag[T], v: c.WeakTypeTag[V]): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(adjustment)
    q"new _root_.com.outr.reactify.Dep[$t, $v]($variable, $adjustment, false, $observables)($connector)"
  }

  def newSubmissiveDep[T, V](c: blackbox.Context)
                  (variable: c.Tree, adjustment: c.Tree)
                  (connector: c.Tree)
                  (implicit t: c.WeakTypeTag[T], v: c.WeakTypeTag[V]): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(adjustment)
    q"new _root_.com.outr.reactify.Dep[$t, $v]($variable, $adjustment, true, $observables)($connector)"
  }
}
