package test

import org.scalatest.{Matchers, WordSpec}
import reactify._

class ValSpec extends WordSpec with Matchers {
  "Vals" should {
    "contain the proper value" in {
      val v = Val(5)
      v() should be(5)
    }
    "contain the proper value when added" in {
      val v1 = Val(5)
      val v2 = Val(v1 + 5)
      v2() should be(10)
    }
    "update properly when referencing a Var" in {
      val v1 = Var(5)
      val v2 = Val(v1 + 5)
      v2() should be(10)
      v1 := 10
      v2() should be(15)
    }
  }
}