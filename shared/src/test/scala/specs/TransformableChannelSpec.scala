package specs

import org.scalatest.{Matchers, WordSpec}
import reactify.TransformableChannel

class TransformableChannelSpec extends WordSpec with Matchers {
  "Transformable" should {
    val simple = TransformableChannel[String]
    var currentValue: String = ""

    "attach an observer to reference the value when it changes" in {
      simple.attach(currentValue = _)
    }
    "attach a transforming observer to reverse values" in {
      simple.transform.attach { value =>
        value.transform(value.value.reverse)
      }
    }
    "send a value and verify it is reversed" in {
      simple := "hello"
      currentValue should be("olleh")
    }
    "clear transforms" in {
      simple.transform.clear()
      currentValue = ""
    }
    "attach a transforming observer to cancel values shorter than four characters" in {
      simple.transform.attach { value =>
        if (value.value.length >= 4) {
          value.continue
        } else {
          value.cancel
        }
      }
    }
    "send a short value" in {
      simple := "one"
      currentValue should be("")
    }
    "send a longer value" in {
      simple := "hello"
      currentValue should be("hello")
    }
  }
}