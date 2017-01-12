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

    def setChannel(value: c.Tree): c.Tree = {
      val observables = retrieveObservables(c)(value)
      val channel = c.prefix.tree

      c.untypecheck(q"$channel.update(List(..$observables), $value)")
    }

    def setStateChannel(value: c.Tree): c.Tree = {
      val observables = retrieveObservables(c)(value)
      val channel = c.prefix.tree
      val selfReference = observables.exists(_.equalsStructure(channel))

      val transformed = if (selfReference) {
        val transformer = new Transformer {
          override def transform(tree: c.universe.Tree): c.universe.Tree = if (tree.equalsStructure(channel)) {
            q"Val(previousValue())"
          } else {
            super.transform(tree)
          }
        }
        transformer.transform(value)
      } else {
        value
      }

      c.untypecheck(q"""
        val previousValue = State.internalFunction($channel)
        $channel.update(List(..$observables), $transformed)
     """)
    }

    if (c.prefix.tree.tpe <:< c.typeOf[StateChannel[_]]) {
      setStateChannel(value)
    } else {
      setChannel(value)
    }
  }

  def mod(c: blackbox.Context)(f: c.Tree): c.Tree = {
    import c.universe._

    val observables = retrieveObservables(c)(f)
    val channel = c.prefix.tree
    q"""
        val previousValue = State.internalFunction($channel)
        $channel.update(List(..$observables), $f(previousValue()))
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
