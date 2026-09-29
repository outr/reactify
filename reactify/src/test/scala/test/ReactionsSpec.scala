package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Channel, Priority, Var}

import scala.language.implicitConversions

class ReactionsSpec extends AnyWordSpec with Matchers {
  "Reactions" should {
    "fire in priority order, first come first within a priority" in {
      val c = Channel[Int]
      var order = List.empty[String]
      c.attach(_ => order = order :+ "a")
      c.attach(_ => order = order :+ "low", Priority.Low)
      c.attach(_ => order = order :+ "high", Priority.High)
      c.attach(_ => order = order :+ "b")
      c.attach(_ => order = order :+ "high2", Priority.High)
      c @= 1
      order should be(List("low", "a", "b", "high", "high2"))
    }
    "keep their order as reactions are taken out and put in" in {
      val c = Channel[Int]
      var order = List.empty[Int]
      val all = (0 until 40).map(i => c.attach(_ => order = order :+ i))
      all.zipWithIndex.foreach { case (r, i) => if (i % 3 != 0) c.reactions -= r }
      c.attach(_ => order = order :+ 100)
      c.attach(_ => order = order :+ -1, Priority.Low)
      c @= 1
      order should be(-1 :: (0 until 40).filter(_ % 3 == 0).toList ::: List(100))
    }
    "hold reactions distinct by equality: one made again from the same function is not added" in {
      val c = Channel[Int]
      val f = (_: Int) => ()
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += reactify.reaction.Reaction[Int](f, Priority.High)
      c.reactions().size should be(2)
    }
    "hold a reaction once, however often it is added" in {
      val c = Channel[Int]
      var fired = 0
      val r = c.attach(_ => fired += 1)
      c.reactions += r
      c.reactions += r
      c.reactions().size should be(1)
      c @= 1
      fired should be(1)
    }
    "say whether a reaction was there to take out" in {
      val c = Channel[Int]
      val r = c.attach(_ => ())
      (c.reactions -= r) should be(true)
      (c.reactions -= r) should be(false)
      c.reactions() should be(Nil)
    }
    "run a fire under way to the end of the list it began with" in {
      val c = Channel[Int]
      var fired = List.empty[String]
      var second: reactify.reaction.Reaction[Int] = null
      c.attach { _ =>
        fired = fired :+ "first"
        c.reactions -= second
        c.attach(_ => fired = fired :+ "late")
      }
      second = c.attach(_ => fired = fired :+ "second")
      c @= 1
      fired should be(List("first", "second"))
      fired = Nil
      c @= 2
      fired should be(List("first", "late"))
    }
    "keep a reaction in two lists apart" in {
      val a = Channel[Int]
      val b = Channel[Int]
      var heard = 0
      val r = a.attach(_ => heard += 1)
      b.reactions += r
      a.reactions -= r
      a @= 1
      b @= 1
      heard should be(1)
      a.reactions() should be(Nil)
      b.reactions().size should be(1)
    }
    "clear, and take reactions again after" in {
      val c = Channel[Int]
      var heard = 0
      val r = c.attach(_ => heard += 1)
      c.reactions.clear()
      c @= 1
      heard should be(0)
      c.reactions += r
      c @= 2
      heard should be(1)
    }
    "add and take out tens of thousands in about the time it takes to make them" in {
      val theme = Var(0)
      val bound = (0 until 60000).map(i => Var(theme() + i))
      theme.reactions().size should be(60000)
      val started = System.nanoTime()
      bound.foreach(v => v.static(v()))
      val ms = (System.nanoTime() - started) / 1000000L
      theme.reactions() should be(Nil)
      // taking each out by a walk of the list was minutes for this many; a bound far above linear's time still tells the
      // two apart on a machine busy with other work (Native took 1049 ms under load)
      ms should be < 10000L
    }
  }
}
