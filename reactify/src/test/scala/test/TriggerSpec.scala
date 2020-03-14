package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.Trigger

class TriggerSpec extends AnyWordSpec with Matchers {
  "Triggers" should {
    "handle simple invocations" in {
      val t = Trigger()
      var invoked = 0
      t.on(invoked += 1)
      t.trigger()
      invoked should be(1)
      t.trigger()
      t.trigger()
      invoked should be(3)
    }
  }
}