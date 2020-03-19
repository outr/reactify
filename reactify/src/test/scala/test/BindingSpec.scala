package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify._
import reactify.bind.Binding

class BindingSpec extends AnyWordSpec with Matchers {
  "Bindings" when {
    "dealing with a simple binding" should {
      val a = Var[String]("a")
      val b = Var[String]("b")
      var binding: Binding[String, String] = null
      "have the proper initial values" in {
        a() should be("a")
        b() should be("b")
      }
      "bind the two values" in {
        binding = a bind b
        a() should be("a")
        b() should be("a")
      }
      "propagate a -> b" in {
        a := "one"
        a() should be("one")
        b() should be("one")
      }
      "propagate b -> a" in {
        b := "two"
        a() should be("two")
        b() should be("two")
      }
      "detach the binding" in {
        binding.detach()
      }
      "verify a -> b no longer propagates" in {
        a := "three"
        a() should be("three")
        b() should be("two")
      }
      "verify b -> a no longer propagates" in {
        b := "four"
        a() should be("three")
        b() should be("four")
      }
    }
    "dealing with binding between different types" should {
      val a = Var[String]("5")
      val b = Var[Int](10)
      var binding: Binding[String, Int] = null

      implicit val s2i: String => Int = (s: String) => Integer.parseInt(s)
      implicit val i2s: Int => String = (i: Int) => i.toString

      "have the proper initial values" in {
        a() should be("5")
        b() should be(10)
      }
      "bind the two values" in {
        binding = a bind b
        a() should be("5")
        b() should be(5)
      }
      "propagate a -> b" in {
        a := "25"
        a() should be("25")
        b() should be(25)
      }
      "propagate b -> a" in {
        b := 50
        a() should be("50")
        b() should be(50)
      }
      "detach the binding" in {
        binding.detach()
      }
      "verify a -> b no longer propagates" in {
        a := "100"
        a() should be("100")
        b() should be(50)
      }
      "verify b -> a no longer propagates" in {
        b := "200"
        a() should be("100")
        b() should be(200)
      }
    }
    "unbinding should work properly" in {
      var v1Fired = 0
      var v2Fired = 0

      val v1 = Var("Hello")
      val v2 = Var("World")
      val binding = v1 bind v2
      v1.attach(_ => v1Fired += 1)
      v2.attach(_ => v2Fired += 1)

      v1Fired should be(0)
      v2Fired should be(0)

      v1 := "Hi"

      v1Fired should be(1)
      v2Fired should be(1)
      v2() should be("Hi")

      v2 := "Universe"

      v2Fired should be(2)
      v1Fired should be(2)
      v1() should be("Universe")

      binding.detach()

      v1 := "Goodbye"

      v1Fired should be(3)
      v2Fired should be(2)
      v2() should be("Universe")

      v2 := "Internet"

      v2Fired should be(3)
      v1Fired should be(3)
      v1() should be("Goodbye")
    }
  }
}
