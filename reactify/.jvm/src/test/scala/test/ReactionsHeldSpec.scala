package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.Channel

/** What a reaction taken out still holds, which only the JVM can see (a weak reference, and a collector to ask). */
class ReactionsHeldSpec extends AnyWordSpec with Matchers {
  "Reactions" should {
    "hold nothing of a reaction taken out, however few it has" in {
      val c = Channel[Int]
      c.attach(_ => ())
      // a reaction holding something, heard once and taken out again
      def heardAndGone(): java.lang.ref.WeakReference[AnyRef] = {
        val held = new Object
        val r = c.attach(_ => held.hashCode())
        c @= 1
        c.reactions -= r
        new java.lang.ref.WeakReference[AnyRef](held)
      }
      val ref = heardAndGone()
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
