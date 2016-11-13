package specs

import com.outr.props.Val
import org.scalatest.{Matchers, WordSpec}

class PropsSpec extends WordSpec with Matchers {
  "Props" when {
    "using a Val" should {
      "contain the proper value" in {
        val v = Val(5)
        v.get should be(5)
      }
//      "container the proper value when added" in {
//        val v1 = Val(5)
//        val v2 = Val(v1 + 5)
//        v2 should be(10)
//      }
    }
  }
}
