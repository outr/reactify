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
    }
    "derive a value from itself depending on another value" in {
      val v1 = Var(1)
      val v2 = Var(v1 + 1)

      v1.reactions() should be(List(v2.state))
      v2.reactions() should be(Nil)

      val v1State1 = v1.state
      val v2State1 = v2.state

      v1State1.value should be(1)
      v1State1.previousState should be(None)
      v2State1.value should be(2)
      v2State1.previousState should be(None)

      v2() should be(2)
      v2 := v2 * 2

      val v2State2 = v2.state
      v2State1 should not be v2State2
      v1.state.value should be(1)
      v2State2.previousState should be(Some(v2State1))
      v2State1.previousState should be(None)
      v2State1.nextState should be(Some(v2.state))

      v2() should be(4)
      v1 := 2

      val v1State2 = v1.state

      // Disconnected because no recurrent reference found
      v1State2.previousState should be(None)
      v1State1.previousState should be(None)

      v2State2.previousState should be(Some(v2State1))
      v2State1.previousState should be(None)
      v2State1.nextState should be(Some(v2State2))
      v2State2.nextState should be(None)

      v2() should be(6)
    }
    "create a variable that builds upon itself multiple times" in {
      val v = Var(1)
      v := v + v + v
      v() should be(3)
    }*/
    "create a list that is dependent on vars" in {
      val s1 = Var("One", Some("s1"))
      val s2 = Var("Two", Some("s2"))
      val list = Var(List.empty[String], Some("list"))
      list := s1() :: s2() :: list()
      list() should be(List("One", "Two"))
      list.state.index should be(2)
      list.state.references.toSet should be(Set(s1.state, s2.state, list.state.previousState.get))
      s2.reactions() should contain(list.state)
      println("*********** SETTING TO THREE!")
      s2 := "Three"
      list.state.index should be(2)
      list() should be(List("One", "Three"))
      s1 := "Two"
      list() should be(List("Two", "Three"))
      list := "One" :: list()
      list() should be(List("One", "Two", "Three"))
      s2 := "Four"
      list() should be(List("One", "Two", "Four"))
    }
  }
}