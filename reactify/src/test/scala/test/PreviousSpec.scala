package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.Var

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
}
