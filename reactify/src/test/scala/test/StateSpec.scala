package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Val, Var}

import scala.util.{Failure, Success}
import scala.language.implicitConversions

/** A value's state, kept as its value or what its function threw: read, failed, recovered and written as it was. */
class StateSpec extends AnyWordSpec with Matchers {
  "A value's state" should {
    "be its value" in {
      val v = Var("a")
      v.option should be(Some("a"))
      v.toTry should be(Success("a"))
      v.toString should be("Var(Success(a))")
      v @= null
      v() should be(null)
      v.option should be(Some(null))
      v.toTry should be(Success(null))
    }
    "be what its function threw, thrown on a read, until it is worked out again" in {
      val boom = new RuntimeException("boom")
      val a = Var(0)
      val v = Val(if (a() == 1) throw boom else a() * 2)
      var heard = List.empty[Int]
      v.attach(i => heard = heard :+ i)
      v() should be(0)
      a @= 1
      (the[RuntimeException] thrownBy v()) should be theSameInstanceAs boom
      v.option should be(None)
      v.toTry should be(Failure(boom))
      v.toString should be(s"Var(Failure($boom))")
      a @= 2
      v() should be(4)
      v.option should be(Some(4))
      heard should be(List(4))
    }
    "fail as it is set by a function that throws, and recover as it is set plainly" in {
      val v = Var(1)
      v := (throw new IllegalStateException("no"))
      an[IllegalStateException] should be thrownBy v()
      v.option should be(None)
      v @= 3
      v() should be(3)
    }
    "be written and read by a subclass as a Try" in {
      val v = new StateProbe[Int](1)
      v.stateNow should be(Success(1))
      v() should be(1)
      val e = new IllegalArgumentException("x")
      v.stateTo(Failure(e))
      v.stateNow should be(Failure(e))
      (the[IllegalArgumentException] thrownBy v()) should be theSameInstanceAs e
      v.option should be(None)
      v.stateTo(Success(5))
      v() should be(5)
      v.stateTo(null)
      v.stateNow should be(null)
      v.option should be(None)
      a[MatchError] should be thrownBy v()
    }
    "be nothing before it is set" in {
      val v = new UnsetProbe[Int]
      v.stateNow should be(null)
      v.option should be(None)
      v.toTry should be(null)
      v.toString should be("Var(null)")
      a[MatchError] should be thrownBy v()
      var heard = List.empty[Int]
      v.attach(i => heard = heard :+ i)
      v @= 1
      heard should be(List(1))
      v() should be(1)
    }
    "fire nothing as it fails, and as it recovers tell its reactions it had no value before" in {
      val a = Var(0)
      val v = Val(if (a() < 0) throw new RuntimeException("negative") else a())
      var heard = List.empty[(Int, Int)]
      v.changes((was, is) => heard = heard :+ (was -> is))
      a @= 1
      a @= -1
      a @= 2
      // (told nothing of what it was, a change reaction goes by the last value it heard)
      heard should be(List(0 -> 1, 1 -> 2))
      v() should be(2)
    }
  }
}
