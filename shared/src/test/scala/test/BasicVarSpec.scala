package test

import org.scalatest.{Matchers, WordSpec}
import reactify._

class BasicVarSpec extends WordSpec with Matchers {
  "Vars" should {
    /*"contain the proper value" in {
      val v = Var("Hello")
      v.get should be("Hello")
    }
    "see a simple change" in {
      var changed = 0
      val v = Var("Hello")
      v.reactions().size should be(0)
      v.on(changed += 1)
      v.reactions().size should be(1)
      v := "Goodbye"
      v() should be("Goodbye")
      changed should be(1)
    }
    "automatically fire a change when a dependency changes" in {
      var changed = 0
      val v1 = Var("Matt")
      v1.reactions().size should be(0)
      val v2 = Var(s"Hello, ${v1()}")
      v1.reactions().size should be(1)
      v2.reactions().size should be(0)
      v2.state.references.size should be(1)
      v2.state.references should be(List(v1.state))
      v2.on(changed += 1)
      v2.reactions().size should be(1)
      v2() should be("Hello, Matt")
      v1 := "Rebecca"
      v2() should be("Hello, Rebecca")
      changed should be(1)
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
    "only fire distinct values" in {
      val v = Var(5)
      var changed = 0
      var latest = v()
      v.attach { value =>
        changed += 1
        latest = value
      }
      v := 5
      changed should be(0)
      latest should be(5)
      v := 6
      changed should be(1)
      latest should be(6)
      v := 6
      changed should be(1)
      latest should be(6)
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
    "observe a simple change immediately with attachAndFire" in {
      val v = Var(5)
      var changed = 0
      v.attachAndFire { value =>
        changed += 1
        value should be(5)
      }
      changed should be(1)
    }
    "observe a change with a ChangeObserver" in {
      val v = Var(5)
      var changes = 0
      var original = 0
      var current = 0
      v.changes {
        case (oldValue, newValue) => {
          original = oldValue
          current = newValue
          changes += 1
        }
      }
      v := 10
      changes should be(1)
      original should be(5)
      current should be(10)
      v := 15
      changes should be(2)
      original should be(10)
      current should be(15)
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
    "derive a value from itself and not explode" in {
      val v = Var(1)
      v() should be(1)
      v := v + 2
      v() should be(3)
      v := v * 4
      v() should be(12)
    }*/
    "derive a value from itself depending on another value" in {
      val v1 = Var(1)
      val v2 = Var(v1 + 1)
      v2() should be(2)
      println("*** 1 ***")
      v2 := v2 * 2
      println("*** 2 ***")
      v2() should be(4)
      v1 := 2
      println("*** 3 ***")
      v2() should be(6)
    }
  }
}