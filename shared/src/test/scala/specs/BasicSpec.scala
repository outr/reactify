package specs

import com.outr.reactify._
import org.scalatest.{Matchers, WordSpec}

class BasicSpec extends WordSpec with Matchers {
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
  }
  "Vals" should {
    "contain the proper value" in {
      val v = Val(5)
      v() should be(5)
      v.value should be(5)
    }
    "contain the proper value when added" in {
      val v1 = Val(5)
      val v2 = Val(v1 + 5)
      v2() should be(10)
    }
    "statically assign properly" in {
      val v1 = Var.apply(5)
      val v2 = Var.apply(5)
      val v3 = Val.static(v1 + v2)
      v3.get should be(10)
      v1 := 10
      v3.get should be(10)
    }
    "update properly when referencing a Var" in {
      val v1 = Var(5)
      val v2 = Val(v1 + 5)
      v2() should be(10)
      v1 := 10
      v2() should be(15)
    }
  }
  "Vars" should {
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
    "only fire distinct values when using 'distinct'" in {
      val v = Var(5)
      var changed = 0
      var latest = v()
      v.distinct.attach { value =>
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
    "statically assign properly" in {
      val v1 = Var.apply(5)
      val v2 = Var.apply(5)
      val v3 = Var.static(v1 + v2)
      v3.get should be(10)
      v1 := 10
      v3.get should be(10)
      v3.setStatic(v1 + v2)
      v3.get should be(15)
      v2 := 10
      v3.get should be(15)
    }
    "assign and get via 'value'" in {
      val v = Var(5)
      v.value should be(5)
      v.value = 10
      v.value should be(10)
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
    "observe a change with a ChangeListener" in {
      val v = Var(5)
      var changes = 0
      var original = 0
      var current = 0
      v.changes(new ChangeListener[Int] {
        override def change(oldValue: Int, newValue: Int) = {
          original = oldValue
          current = newValue
          changes += 1
        }
      })
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
      val v = Var(5)
      v := v + 5
      v() should be(10)
      v := v + 5
      v() should be(15)
    }
    "create a list that is dependent on vars" in {
      val s1 = Var("One")
      val s2 = Var("Two")
      val list = Var(List.empty[String])
      list := s1() :: s2() :: list()
      list() should be(List("One", "Two"))
      s2 := "Three"
      list() should be(List("One", "Three"))
      s1 := "Two"
      list() should be(List("Two", "Three"))
      list := "One" :: list()
      list() should be(List("One", "Two", "Three"))
      s2 := "Four"
      list() should be(List("One", "Two", "Four"))
    }
    "create a Container with a generic Child list" in {
      val v1 = Var("One")
      val v2 = Var("Two")
      val container = new Container[String]
      container.children := Vector(v1, v2)
      container.children.observing should be(List(v1, v2))
      container.children() should be(Vector("One", "Two"))
      v1 := "First"
      v2 := "Second"
      container.children() should be(Vector("First", "Second"))
    }
    "create an advanced derived value" in {
      val v1 = Var(10)
      val v2 = Var("Yes")
      val v3 = Var("No")
      val complex = Val[String] {
        if (v1 > 10) {
          v2
        } else {
          v3
        }
      }
      complex.observables should be(List(v1, v2, v3))
      var current = complex()
      complex.attach(current = _)

      complex() should be("No")
      current should be("No")

      v1 := 15
      complex() should be("Yes")
      current should be("Yes")

      v2 := "True"
      v3 := "False"
      complex() should be("True")
      current should be("True")

      v1 := 5
      complex() should be("False")
      current should be("False")
    }
  }

  class Container[Child] {
    val children: Var[Vector[Child]] = Var[Vector[Child]](Vector.empty)
  }
}