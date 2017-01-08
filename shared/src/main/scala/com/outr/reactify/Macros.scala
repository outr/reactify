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

    /*def processChildren(children: List[c.Tree]): List[c.Tree] = children.map { child =>
      if (child.equalsStructure(channel)) {
        q"previousValue"
      } else if (child.children.nonEmpty) {
        val sym = child.symbol
        val updated = processChildren(child.children)
        q"$sym(..$updated)"
      } else {
        child
      }
    }

    val sym = value.symbol
    val updated = processChildren(value.children)
    val u = q"$sym(..$updated)"
    println(s"Value: $value, Updated: $u")*/

    /*println("---------------------")
    value match {
      case t: Apply => {
        println(value)
        t.
        t.args.foreach { arg =>
          println(s"arg: $arg")
        }
      }
      case t: Literal =>
    }*/
//    var updated = value
    /*observables.foreach { o =>
      if (o.equalsStructure(channel)) {
        println(s"Found Reference to channel: $o, Position: ${o.pos}, String: $value")
        o.pos.update(q"previousValue")
      }
    }
    println(value)*/

    observables.foreach { o =>
      if (o.equalsStructure(channel)) {
        c.abort(c.enclosingPosition, s"Recursive reference detected ($channel) in assignment (including self in the assignment function). $value")
      }
    }
//    val updated = value.collect {
//      case t if t.equalsStructure(channel) => q"previousValue"
//      case t => t
//    }
//    val u = Apply(value.symbol, updated: _*)
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
