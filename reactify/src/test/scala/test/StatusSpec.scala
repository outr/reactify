package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Channel, Reactive, Var}
import reactify.reaction.ReactionStatus

class StatusSpec extends AnyWordSpec with Matchers {
  "A propagation's status" should {
    "be there only while the value's own reactions run" in {
      val a = Var(0)
      val b = Var(0)
      var seen = List.empty[(Option[ReactionStatus], Option[ReactionStatus])]
      a.attach(_ => seen = seen :+ (a.status -> b.status))
      a.status should be(None)
      a @= 1
      seen should be(List(Some(ReactionStatus.Continue) -> None))
      a.status should be(None)
    }
    "not be set outside a propagation" in {
      val a = Var(0)
      an[AssertionError] should be thrownBy a.stopPropagation()
    }
    "stop the later reactions once stopped" in {
      val c = Channel[Int]
      var heard = List.empty[String]
      c.attach(i => if (i % 2 == 0) c.stopPropagation())
      c.attach(i => heard = heard :+ s"second $i")
      c @= 1
      c @= 2
      c @= 3
      heard should be(List("second 1", "second 3"))
    }
    "be stopped from a propagation of another value nested in it" in {
      val a = Channel[Int]
      val b = Channel[Int]
      var heard = List.empty[String]
      b.attach(_ => a.stopPropagation())
      b.attach(i => heard = heard :+ s"b $i")
      a.attach(i => b @= i)
      a.attach(i => heard = heard :+ s"a $i")
      a @= 1
      heard should be(List("b 1"))
      b.status should be(None)
      a.status should be(None)
    }
    "end with a propagation of the same value nested in it: the older value reaches no later reaction" in {
      val v = Var(0)
      var heard = List.empty[Int]
      v.attach(i => if (i == 1) v @= 2)
      v.attach(i => heard = heard :+ i)
      v @= 1
      heard should be(List(2))
      v() should be(2)
      v @= 3
      heard should be(List(2, 3))
    }
    "end with a fire of the same value with no reactions nested in it" in {
      val c = Channel[Int]
      var heard = List.empty[Int]
      c.attach(i => Reactive.fire(c, i, None, Nil))
      c.attach(i => heard = heard :+ i)
      c @= 1
      heard should be(Nil)
      c.status should be(None)
    }
    "be gone after a reaction throws" in {
      val c = Channel[Int]
      c.attach(i => if (i == 1) throw new RuntimeException("boom"))
      var heard = List.empty[Int]
      c.attach(i => heard = heard :+ i)
      a[RuntimeException] should be thrownBy (c @= 1)
      c.status should be(None)
      c @= 2
      heard should be(List(2))
    }
  }
}
