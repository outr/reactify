package reactify

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** A value with no state yet makes no Throwable until it is read: making one as the first value is made (an app's
  * start) was a stack trace taken there, which a browser with DevTools open works out source positions for. */
class UnsetSpec extends AnyWordSpec with Matchers {
  "A value with no state yet" should {
    "hold no Throwable, and throw a MatchError of its own each time it is read" in {
      Val.Unset.isInstanceOf[Throwable] should be(false)
      val v = new test.UnsetProbe[Int]
      v.heldFailure should be theSameInstanceAs Val.Unset
      val first = the[MatchError] thrownBy v()
      val second = the[MatchError] thrownBy v()
      first should not be theSameInstanceAs(second)
    }
  }
}
