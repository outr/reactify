package specs

import org.scalatest.{Matchers, WordSpec}
import reactify.Transformable

class TransformableSpec extends WordSpec with Matchers {
  "Transformable" should {
    val simple = Transformable[String]
    var currentValue: String = ""

    "attach a listener to reference the value when it changes" in {
      simple.attach(currentValue = _)
    }
    "attach a transforming listener to reverse values" in {
      simple.transform.attach { value =>
        value.transform(value.value.reverse)
      }
    }
    "send a value and verify it is reversed" in {
      simple := "hello"
      currentValue should be("olleh")
    }
    // TODO: test cancel and continue
  }
}