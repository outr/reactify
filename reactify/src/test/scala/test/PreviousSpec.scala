package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Val, Var}

import scala.language.implicitConversions

/** A value set plainly tells its reactions what it was, and a function reading its own value builds on the value as it
  * is: that it keeps nothing of what it was after is PreviousHeldSpec, where there is a collector to ask (the JVM). */
class PreviousSpec extends AnyWordSpec with Matchers {
  "A value set plainly" should {
    "tell its reactions what it was" in {
      val screen = new Object
      val v = Var[Option[AnyRef]](Some(screen))
      var heard: Option[AnyRef] = None
      v.changes((was, _) => heard = was)
      v @= None
      heard.exists(_ eq screen) should be(true)
    }
    "be read as it is by a function that reads it, evaluated again" in {
      val v = Var(0)
      val o = Var(0)
      v @= 5
      v := v() + o()
      o @= 2
      v() should be(7)
    }
  }
  "A value worked out by a function" should {
    "tell its reactions what it was" in {
      val a = Var(1)
      val v = Val(a() * 10)
      var heard = List.empty[(Option[Int], Int)]
      v.reactions += new reactify.reaction.Reaction[Int] {
        override def apply(value: Int, previous: Option[Int]): reactify.reaction.ReactionStatus = {
          heard = heard :+ (previous -> value)
          reactify.reaction.ReactionStatus.Continue
        }
      }
      a @= 2
      a @= 3
      heard should be(List(Some(10) -> 20, Some(20) -> 30))
    }
    "tell its reactions it was null, where it was" in {
      val a = Var[String](null)
      val v = Val(if (a() == null) null else a() + "!")
      var heard = List.empty[(Option[String], String)]
      v.reactions += new reactify.reaction.Reaction[String] {
        override def apply(value: String, previous: Option[String]): reactify.reaction.ReactionStatus = {
          heard = heard :+ (previous -> value)
          reactify.reaction.ReactionStatus.Continue
        }
      }
      a @= "x"
      heard should be(List(Some(null) -> "x!"))
    }
    "read, where it reads itself, what it was before its last change" in {
      val v = Var(1)
      val o = Var(10)
      v := v() + o()
      v() should be(11)
      o @= 20
      v() should be(21)
      o @= 30
      v() should be(41)
      v.changes((_, _) => ())
      o @= 31
      v() should be(52)
    }
    "read null, where it reads itself, where it was null before its last change" in {
      val v = Var[String](null)
      val o = Var("a")
      v := (if (v() == null) "first " else v()) + o()
      v() should be("first a")
      o @= "b"
      v() should be("first b")
      o @= "c"
      v() should be("first ac")
    }
    "read itself as it is where it was last set plainly, and as it was before its last change after" in {
      val v = Var(0)
      val o = Var(0)
      val heard = scala.collection.mutable.ListBuffer.empty[(Int, Int)]
      v.changes((was, is) => heard += (was -> is))
      v @= 5
      v := v() + o()
      o @= 2
      o @= 3
      v() should be(8)
      heard.toList should be(List(0 -> 5, 5 -> 7, 7 -> 8))
    }
    "be written and read by a subclass as an Option" in {
      val v = new StateProbe[String]("a")
      v.previousNow should be(None)
      v.previousTo(Some("b"))
      v.previousNow should be(Some("b"))
      v.previousTo(Some(null))
      v.previousNow should be(Some(null))
      v.previousTo(None)
      v.previousNow should be(None)
    }
  }
}
