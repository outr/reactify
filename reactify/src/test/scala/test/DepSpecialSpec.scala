package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify._

import scala.language.implicitConversions

class DepSpecialSpec extends AnyWordSpec with Matchers {
  "Deps Special Use-Cases" when {
    "combining Ints" should {
      "do simple addition" in {
        val a = Var(5)
        val b = Var(10)
        val combined = Dep[Int, Int](a)(_ + b, _ - b)
        combined() should be(15)

        a := 10
        combined() should be(20)
      }
      "support customized connector" in {
        val a = Var(5)
        val b = Var(10)
        val distance = Dep[Int, Int](a)(v => math.abs(v - b), dist => b - dist)

        distance() should be(5)

        a := 20
        distance() should be(10)

        distance := 8
        distance() should be(8)
        a() should be(2)
        b() should be(10)
      }
    }
    "validating derived observables" should {
      val left = Var(0.0)
      val width = Var(0.0)
      val center = Dep[Double, Double](left)(_ + width / 2.0, _ - width / 2.0)
      val right = Dep[Double, Double](left)(_ + width, _ - width)

      var leftValue = left()
      left.attach(d => leftValue = d)

      def verify(l: Double, c: Double, r: Double, w: Double, v: Double): Unit = {
        val list = List(l, c, r, w, v)
        List(left(), center(), right(), width(), leftValue) should be(list)
      }

      "update center" in {
        center := 500.0
        verify(500.0, 500.0, 500.0, 0.0, 500.0)
      }
      "update width" in {
        width := 100.0
        verify(450.0, 500.0, 550.0, 100.0, 450.0)
      }
//      "increment center from itself" in {
//        center := center() + 10.0
//        verify(460.0, 510.0, 560.0, 100.0, 460.0)
//      }
    }
    "propagation of values" should {
      val left = Var(0.0)
      val width = Var(0.0)
      val center = Dep[Double, Double](left)(_ + width / 2.0, _ - width / 2.0)
      val right = Dep[Double, Double](left)(_ + width, _ - width)

      val arbitrary = Var(0.0)

      "set the right to an arbitrary value" in {
        right := arbitrary
        arbitrary := 100.0

        List(left(), center(), right()) should be(List(100.0, 100.0, 100.0))

        width := 50.0

        List(left(), center(), right()) should be(List(50.0, 75.0, 100.0))
      }
    }
  }
}
