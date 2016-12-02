package specs

import com.outr.props._
import org.scalatest.{Matchers, WordSpec}

class DepsSpec extends WordSpec with Matchers {
  val left: Var[Double] = Var(0.0)
  val width: Var[Double] = Var(0.0)
  val right: Dep[Double, Double] = Dep(left + width, left, _ - width)

  "Deps" should {
    "have the basic values" in {
      left() should be(0.0)
      width() should be(0.0)
      right() should be(0.0)
    }
    "set left and reflect properly in right" in {
      left := 50.0
      left() should be(50.0)
      width() should be(0.0)
      right() should be(50.0)
    }
    "set width and reflect properly in right" in {
      width := 25.0
      left() should be(50.0)
      width() should be(25.0)
      right() should be(75.0)
    }
    "set right and reflect properly in left" in {
      right := 100.0
      left() should be(75.0)
      width() should be(25.0)
      right() should be(100.0)
    }
  }
}
