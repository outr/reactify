package test

import org.scalatest.{Matchers, WordSpec}
import reactify.Var

class BasicVarSpec extends WordSpec with Matchers {
  "Vars" should {
    "create a simple Var" in {
      val v = Var("Hello")
      v.get should be("Hello")
    }
    "see a simple change" in {
      var changed = 0
      val v = Var("Hello")
      v.on(changed += 1)
      v := "Goodbye"
      v() should be("Goodbye")
      changed should be(1)
    }
    "automatically fire a change when a dependency changes" in {
      var changed = 0
      val v1 = Var("Matt")
      val v2 = Var(s"Hello, ${v1()}")
      v2.on(changed += 1)
      v2() should be("Hello, Matt")
      v1 := "Rebecca"
      v2() should be("Hello, Rebecca")
      changed should be(1)
    }
  }
}