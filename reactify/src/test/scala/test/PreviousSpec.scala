package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.Var

import java.lang.ref.WeakReference
import scala.language.implicitConversions

class PreviousSpec extends AnyWordSpec with Matchers {
  "A value set plainly" should {
    "tell its reactions what it was, and keep nothing of it after" in {
      var screen: AnyRef = new Object
      val ref = new WeakReference(screen)
      val v = Var[Option[AnyRef]](Some(screen))
      var heard: Option[AnyRef] = None
      v.changes((was, _) => heard = was)
      v @= None
      heard.exists(_ eq ref.get()) should be(true)
      heard = None
      screen = null
      var tries = 0
      while (ref.get() != null && tries < 20) {
        System.gc()
        Thread.sleep(10L)
        tries += 1
      }
      ref.get() should be(null)
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
