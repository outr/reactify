package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Val, Var}
import reactify.transaction.Transaction

import scala.language.implicitConversions

/** A value set as it is (`@=`, `static`) keeps no function for it: evaluated, it comes to the value it has, and a
  * transaction puts it back as it was. */
class StaticSpec extends AnyWordSpec with Matchers {
  "A value set as it is" should {
    "evaluate to the value it has, firing nothing" in {
      val v = Var(1)
      v @= 2
      var heard = List.empty[Int]
      v.attach(i => heard = heard :+ i)
      Val.evaluate(v, updating = true)
      Val.evaluate(v, updating = false)
      v() should be(2)
      heard should be(Nil)
    }
    "stop following what it read, and be read by what follows it" in {
      val a = Var(1)
      val v = Var(0)
      v := a() * 10
      val b = Val(v() + 1)
      v @= 5
      b() should be(6)
      a @= 2
      v() should be(5)
      b() should be(6)
      v @= 7
      b() should be(8)
    }
    "be heard by a reaction to what it read already under way as it was set" in {
      val a = Var(1)
      val v = Var(0)
      v := a() * 10
      var heard = List.empty[Int]
      // the first reaction on `a` (Low runs first) sets v plainly; v's own, under way in the same fire, evaluates v with no function
      a.attach(i => if (i == 2) v @= 99, reactify.Priority.Low)
      v.attach(i => heard = heard :+ i)
      a @= 2
      v() should be(99)
      heard should be(List(99))
    }
    "be put back by a transaction undone, and set again by it redone" in {
      val v = Var("one")
      v @= "two"
      val t = Transaction {
        v @= "three"
        v @= "four"
      }
      v() should be("four")
      t.undo()
      v() should be("two")
      t.redo()
      v() should be("four")
    }
    "be put back by a transaction of a function undone" in {
      val a = Var(1)
      val v = Var(0)
      v @= 3
      val t = Transaction {
        v := a() + 10
      }
      v() should be(11)
      a @= 2
      v() should be(12)
      t.undo()
      v() should be(3)
      a @= 5
      v() should be(3)
      t.redo()
      v() should be(15)
    }
    "put back a function by a transaction of a plain set undone" in {
      val a = Var(1)
      val v = Var(0)
      v := a() + 10
      val t = Transaction {
        v @= 7
      }
      v() should be(7)
      t.undo()
      v() should be(11)
      a @= 2
      v() should be(12)
    }
    "set by a Val's own static, keep what it had as its previous, and fire nothing" in {
      val v = new ValProbe[Int](1)
      var heard = 0
      v.attach(_ => heard += 1)
      v.setStatic(2)
      v() should be(2)
      v.previousNow should be(Some(1))
      v.functionNow should be(null)
      heard should be(0)
    }
    "keep no function, where a function was given" in {
      val v = new StateProbe[Int](1)
      v.functionNow should not be null
      v @= 2
      v.functionNow should be(null)
      v := 3
      v.functionNow should not be null
    }
  }
}
