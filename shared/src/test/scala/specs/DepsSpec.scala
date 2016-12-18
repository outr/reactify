package specs

import com.outr.props._
import org.scalatest.{Matchers, WordSpec}

class DepsSpec extends WordSpec with Matchers {
  val width: Var[Double] = Var(0.0)

  val left: Var[Double] = Var(0.0)
  val center: Dep[Double, Double] = Dep(left, width / 2.0)
  val right: Dep[Double, Double] = Dep(left, width, submissive = true)

  "Deps" should {
    "have the basic values" in {
      left() should be(0.0)
      width() should be(0.0)
      center() should be(0.0)
      right() should be(0.0)
    }
    "set left and reflect properly in right" in {
      left := 50.0
      left() should be(50.0)
      width() should be(0.0)
      center() should be(50.0)
      right() should be(50.0)
    }
    "set width and reflect properly in right" in {
      width := 25.0
      left() should be(50.0)
      width() should be(25.0)
      center() should be(62.5)
      right() should be(75.0)
    }
    "set right and reflect properly in left" in {
      right := 100.0
      left() should be(75.0)
      width() should be(25.0)
      center() should be(87.5)
      right() should be(100.0)
    }
    "set width again and not change left" in {
      width := 50.0
      left() should be(75.0)
      width() should be(50.0)
      center() should be(100.0)
      right() should be(125.0)
    }
    "set center and reflect properly in left and right" in {
      center := 200.0
      left() should be(175.0)
      width() should be(50.0)
      center() should be(200.0)
      right() should be(225.0)
    }
    "set width another time and retain center" in {
      width := 100.0
      left() should be(150.0)
      width() should be(100.0)
      center() should be(200.0)
      right() should be(250.0)
    }
    "set left and verify center and right adjust" in {
      left := 100.0
      left() should be(100.0)
      width() should be(100.0)
      center() should be(150.0)
      right() should be(200.0)
    }
    "set width and retain left's position" in {
      width := 500.0
      left() should be(100.0)
      width() should be(500.0)
      center() should be(350.0)
      right() should be(600.0)
    }
  }
}
