package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.Var

import java.lang.ref.WeakReference
import scala.language.implicitConversions

/** A value set plainly keeps nothing of what it was once its reactions have heard it: a screen left is let go of then,
  * not at the value's next change. A weak reference and the collector, which only the JVM has to ask. */
class PreviousHeldSpec extends AnyWordSpec with Matchers {
  "A value set plainly" should {
    "keep nothing of what it was once heard" in {
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
  }
}
