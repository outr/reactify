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
    "keep their order and distinctness as a list grows past its index and shrinks back" in {
      val c = Channel[Int]
      var order = List.empty[Int]
      val f = (_: Int) => ()
      val all = (0 until 100).map(i => c.attach(_ => order = order :+ i, if (i % 10 == 0) Priority.High else Priority.Normal))
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions().size should be(101)
      c @= 1
      order should be((0 until 100).filter(_ % 10 != 0).toList ::: (0 until 100 by 10).toList)
      all.drop(3).foreach(r => c.reactions -= r)
      c.reactions().size should be(4)
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions().size should be(4)
      order = Nil
      c @= 2
      order should be(List(1, 2, 0))
    }
    "hold reactions of their own making by identity: two equal by their fields are both held, each once" in {
      val c = Channel[Int]
      var heard = List.empty[String]
      val hear = (s: String) => heard = heard :+ s
      val a = Tagged("x", hear)
      val b = Tagged("x", hear)
      a should be(b)
      c.reactions += a
      c.reactions += b
      c.reactions += a
      c.reactions() should have size 2
      c @= 1
      heard should be(List("x", "x"))
      (c.reactions -= b) should be(true)
      c.reactions().head should be theSameInstanceAs a
      // and the same past the index
      (0 until 40).foreach(i => c.attach(x => if (x == i) ()))
      c.reactions += a
      c.reactions += b
      c.reactions += b
      c.reactions() should have size 42
    }
    "hold a function reaction once for its function and priority past the index" in {
      val c = Channel[Int]
      (0 until 40).foreach(i => c.attach(x => if (x == i) ()))
      val f = (_: Int) => ()
      val g = (_: Int, _: Int) => ()
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += reactify.reaction.Reaction[Int](f, Priority.High)
      c.reactions += reactify.reaction.Reaction.changes[Int](g)
      c.reactions += reactify.reaction.Reaction.changes[Int](g)
      c.reactions() should have size 43
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
    "hold one reaction, then many, then one and none again, in order throughout" in {
      val c = Channel[Int]
      var heard = List.empty[String]
      def fired(i: Int): List[String] = {
        heard = Nil
        c @= i
        heard
      }
      fired(0) should be(Nil)
      c.reactions() should be(Nil)
      val a = c.attach(_ => heard = heard :+ "a")
      c.reactions() should be(List(a))
      fired(1) should be(List("a"))
      val low = c.attach(_ => heard = heard :+ "low", Priority.Low)
      c.reactions() should be(List(low, a))
      fired(2) should be(List("low", "a"))
      val high = c.attach(_ => heard = heard :+ "high", Priority.High)
      val b = c.attach(_ => heard = heard :+ "b")
      fired(3) should be(List("low", "a", "b", "high"))
      (c.reactions -= low) should be(true)
      (c.reactions -= a) should be(true)
      (c.reactions -= high) should be(true)
      c.reactions() should be(List(b))
      fired(4) should be(List("b"))
      // one left: a reaction of higher priority goes after it, one of lower before
      val low2 = c.attach(_ => heard = heard :+ "low2", Priority.Low)
      fired(5) should be(List("low2", "b"))
      (c.reactions -= b) should be(true)
      (c.reactions -= b) should be(false)
      (c.reactions -= low2) should be(true)
      (c.reactions -= low2) should be(false)
      c.reactions() should be(Nil)
      fired(6) should be(Nil)
      c.attach(_ => heard = heard :+ "again")
      fired(7) should be(List("again"))
    }
    "add a reaction of lower priority before the one there" in {
      val c = Channel[Int]
      var heard = List.empty[String]
      c.attach(_ => heard = heard :+ "normal")
      c.attach(_ => heard = heard :+ "low", Priority.Low)
      c @= 1
      heard should be(List("low", "normal"))
    }
    "hold one reaction distinct, by identity and by function" in {
      val c = Channel[Int]
      val f = (_: Int) => ()
      val r = c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += reactify.reaction.Reaction[Int](f)
      c.reactions += r
      c.reactions() should be(List(r))
      c.reactions += reactify.reaction.Reaction[Int](f, Priority.High)
      c.reactions().size should be(2)
      var heard = List.empty[String]
      val hear = (s: String) => heard = heard :+ s
      val v = Var(0)
      v.reactions += Tagged("x", hear)
      v.reactions += Tagged("x", hear)
      v.reactions().size should be(2)
      v @= 1
      heard should be(List("x", "x"))
    }
    "take out a reaction that is not there, with one there, saying so" in {
      val c = Channel[Int]
      val a = c.attach(_ => ())
      val other = Channel[Int].attach(_ => ())
      (c.reactions -= other) should be(false)
      c.reactions() should be(List(a))
    }
    "fire with none, and none after the last is taken out, on every kind of value" in {
      val v = Var(0)
      val c = Channel[Int]
      val t = reactify.Trigger()
      v @= 1
      c @= 1
      t.trigger()
      val rv = v.attach(_ => ())
      val rc = c.attach(_ => ())
      val rt = t.attach(_ => ())
      v.reactions -= rv
      c.reactions -= rc
      t.reactions -= rt
      v @= 2
      c @= 2
      t.trigger()
      v.reactions() should be(Nil)
      c.reactions() should be(Nil)
      t.reactions() should be(Nil)
      v() should be(2)
    }
    "be heard where a value's reactions are its own kind (overridden)" in {
      val inner = Var(0)
      val own = new OwnReactionsProbe(inner)
      var heard = List.empty[Int]
      own.attach(i => heard = heard :+ i)
      own.reactions().size should be(1)
      own.reactions.isInstanceOf[CountingReactions[_]] should be(true)
      inner @= 1
      inner @= 2
      heard should be(List(1, 2))
      own.added should be(1)
    }
    "be heard by a group's items, and through a Var group's own set" in {
      val a = Var(0)
      val b = Var(0)
      val group = a & b
      var heard = List.empty[Int]
      val r = group.attach(i => heard = heard :+ i)
      a @= 1
      b @= 2
      heard should be(List(1, 2))
      group.reactions -= r
      a @= 3
      heard should be(List(1, 2))
    }
    "be taken out by a reaction once, and stay out" in {
      val v = Var(0)
      var heard = List.empty[Int]
      v.once(i => heard = heard :+ i)
      v.reactions().size should be(1)
      v @= 1
      v @= 2
      heard should be(List(1))
      v.reactions() should be(Nil)
    }
    "keep its order as one taken out of many leaves one, while a fire is under way" in {
      val c = Channel[Int]
      var heard = List.empty[String]
      var b: reactify.reaction.Reaction[Int] = null
      val a = c.attach { _ =>
        heard = heard :+ "a"
        c.reactions -= b
      }
      b = c.attach(_ => heard = heard :+ "b")
      c @= 1
      heard should be(List("a", "b"))
      c.reactions() should be(List(a))
      heard = Nil
      c @= 2
      heard should be(List("a"))
    }
  }
}
