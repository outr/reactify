package test

import testy._
import reactify.Trigger

import scala.language.implicitConversions

class TriggerSpec extends Spec {
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