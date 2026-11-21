package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Channel, DeferralProbe, Val, Var}

class DeferralSpec extends AnyWordSpec with Matchers {
  /** A channel whose reactions run as deep as the budget goes: each fire from them is deferred. */
  private def atBudget(): (Channel[Int], Channel[Int]) = {
    val top = Channel[Int]
    var link = top
    (1 until DeferralProbe.maxDepth).foreach { _ =>
      val next = Channel[Int]
      link.attach(i => next @= i)
      link = next
    }
    (top, link)
  }

  /** `count` channels, each recording its index in `heard` when fired. */
  private def recorders(count: Int, heard: () => List[Int] => Unit): Vector[Channel[Int]] =
    (0 until count).toVector.map { index =>
      val c = Channel[Int]
      c.attach(_ => heard()(List(index)))
      c
    }

  "Links deferred past the depth budget" should {
    "run in the order they were deferred, many of them" in {
      val (top, deep) = atBudget()
      var heard = List.empty[Int]
      val channels = recorders(10000, () => more => heard = more ::: heard)
      deep.attach(_ => channels.foreach(_ @= 1))
      top @= 1
      heard.reverse should be((0 until 10000).toList)
      DeferralProbe.waiting should be(0)
    }
    "be deferred and run in time linear in their number" in {
      // appending each to a list was quadratic: 200,000 took minutes; held as two lists, a few milliseconds
      val (top, deep) = atBudget()
      var count = 0
      val c = Channel[Int]
      c.attach(_ => count += 1)
      deep.attach(_ => (1 to 200000).foreach(_ => c @= 1))
      val start = System.nanoTime()
      top @= 1
      val seconds = (System.nanoTime() - start) / 1e9
      count should be(200000)
      seconds should be < 20.0
    }
    "keep those deferred while draining behind those already waiting" in {
      val (top, deep) = atBudget()
      var heard = List.empty[String]
      val (aTop, aDeep) = atBudget()
      val a = Channel[Int]
      val b = Channel[Int]
      val a1 = Channel[Int]
      val a2 = Channel[Int]
      a.attach(_ => { heard = "a" :: heard; aTop @= 1 })
      aDeep.attach(_ => { a1 @= 1; a2 @= 1 })
      b.attach(_ => heard = "b" :: heard)
      a1.attach(_ => heard = "a1" :: heard)
      a2.attach(_ => heard = "a2" :: heard)
      deep.attach(_ => { a @= 1; b @= 1 })
      top @= 1
      heard.reverse should be(List("a", "b", "a1", "a2"))
    }
    "not be left for the next fire when one of them throws" in {
      val (top, deep) = atBudget()
      var heard = List.empty[Int]
      val channels = recorders(10, () => more => heard = more ::: heard)
      channels(1).attach(_ => throw new RuntimeException("deferred"))
      deep.attach(_ => channels.foreach(_ @= 1))
      a[RuntimeException] should be thrownBy (top @= 1)
      DeferralProbe.waiting should be(0)
      heard should be(List(1, 0))
      val other = Channel[Int]
      var otherHeard = 0
      other.attach(_ => otherHeard += 1)
      other @= 1
      otherHeard should be(1)
      heard should be(List(1, 0))
    }
    "not be left for the next fire when the chain that deferred them throws" in {
      val (top, deep) = atBudget()
      var heard = List.empty[Int]
      val channels = recorders(5, () => more => heard = more ::: heard)
      deep.attach(_ => {
        channels.foreach(_ @= 1)
        throw new RuntimeException("deep")
      })
      a[RuntimeException] should be thrownBy (top @= 1)
      DeferralProbe.waiting should be(0)
      val other = Channel[Int]
      other.attach(_ => ())
      other @= 1
      heard should be(Nil)
    }
    "keep a very deep chain off the stack" in {
      val source = Var(0)
      var chain: Val[Int] = source
      (1 to 100000).foreach { _ =>
        val before = chain
        chain = Val(before() + 1)
      }
      val end = chain
      var seen = -1
      end.attach(v => seen = v)
      source @= 1
      seen should be(100001)
      end() should be(100001)
      DeferralProbe.waiting should be(0)
    }
  }
}
