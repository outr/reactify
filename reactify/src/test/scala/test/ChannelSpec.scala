package test

import org.scalatest.{Matchers, WordSpec}
import reactify.Channel

class ChannelSpec extends WordSpec with Matchers {
  "Channels" should {
    "notify when changed" in {
      var changes = 0
      var lastChange: Option[String] = None
      val channel = Channel[String]
      channel.attach { s =>
        changes += 1
        lastChange = Some(s)
      }
      changes should be(0)
      lastChange should be(None)
      channel := "Test 1"
      changes should be(1)
      lastChange should be(Some("Test 1"))
      channel := "Test 2"
      changes should be(2)
      lastChange should be(Some("Test 2"))
    }
    "map a Channel to another typed Channel" in {
      val c1 = Channel[String]
      val c2 = c1.map(_.reverse)

      var lastValue: Option[String] = None
      c2.attach(v => lastValue = Some(v))

      c1 := "Testing"
      lastValue should be(Some("gnitseT"))
    }
    "collect a Channel to another typed Channel" in {
      val IntRegex = """(\d+)""".r
      val c1 = Channel[String]
      val c2 = c1.collect {
        case IntRegex(v) => v.toInt
      }

      var lastValue: Option[Int] = None
      c2.attach(v => lastValue = Some(v))

      c1 := "Testing"
      lastValue should be(None)

      c1 := "50"
      lastValue should be(Some(50))
    }
    "use channel grouping" in {
      val c1 = Channel[String]
      val c2 = Channel[String]

      var latest = ""
      c1.and(c2).attach { value =>
        latest = value
      }

      latest should be("")
      c1 := "one"
      latest should be("one")
      c2 := "two"
      latest should be("two")
    }
  }
}