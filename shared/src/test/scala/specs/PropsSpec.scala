package specs

import com.outr.reactify._
import org.scalatest.{Matchers, WordSpec}

class PropsSpec extends WordSpec with Matchers {
  "Props" when {
    "using a Val" should {
      "contain the proper value" in {
        val v = Val(5)
        v() should be(5)
      }
      "contain the proper value when added" in {
        val v1 = Val(5)
        val v2 = Val(v1 + 5)
        v2() should be(10)
      }
    }
    "using a Var" should {
      "container the proper value" in {
        val v = Var(5)
        v() should be(5)
      }
      "contain the proper value when added" in {
        val v1 = Var(5)
        val v2 = Var(v1 + 5)
        v2() should be(10)
      }
      "contain the proper value when modified" in {
        val v1 = Var(5)
        val v2 = Var(v1 + 5)
        v2.get should be(10)
        v1 := 10
        v2() should be(15)
      }
      "observe a simple change" in {
        val v = Var(5)
        var changed = 0
        var currentValue = v.get
        v.attach { updated =>
          changed += 1
          currentValue = updated
        }
        v := 10
        currentValue should be(10)
        changed should be(1)
      }
      "observe a complex change" in {
        val v1 = Var(5)
        val v2 = Var(10)
        val v3 = Var(v1 + v2)
        var changed = 0
        var currentValue = v3.get
        v3.attach { updated =>
          changed += 1
          currentValue = updated
        }
        v2 := 5
        changed should be(1)
        currentValue should be(10)
        v3.get should be(10)
      }
//      "use syntactic sugar to add to a list" in {
//        val v = Var[List[Int]](Nil)
//        v += 5
//        v.length should be(1)
//        v.head should be(5)
//      }
//      "derive a value from itself and not explode" in {
//        val v = Var(5)
//        v := v() + 5
//        v() should be(10)
//      }
    }
  }
}
