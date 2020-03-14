package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify._
import reactify.group.VarGroup

import scala.collection.mutable.ListBuffer

class VarSpec extends AnyWordSpec with Matchers {
  lazy val lazyDouble: Var[Double] = Var(0.0)

  "Vars" should {
    "contain the proper value" in {
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
    }
    "create a list that is dependent on vars" in {
      val s1 = Var("One", name = Some("s1"))
      val s2 = Var("Two", name = Some("s2"))
      val list = Var(List.empty[String], name = Some("list"))
      list := s1() :: s2() :: list()
      list() should be(List("One", "Two"))
      list.state.index should be(2)
      list.state.references.toSet should be(Set(s1.state, s2.state, list.state.previousState.get))
      s2.reactions() should contain(list.state)
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
    "create a Container with a generic Child list" in {
      val v1 = Var("One")
      val v2 = Var("Two")
      val container = new Container[String]
      container.children := Vector(v1, v2)
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
    "create a nested scenario" in {
      val users = Var[List[User]](Nil)

      val adam = new User {
        name := "Adam"
      }
      val betty = new User {
        name := "Betty"
      }
      val chris = new User {
        name := "Chris"
      }
      val debby = new User {
        name := "Debby"
      }
      users := List(adam, betty, chris, debby)

      val fiveLetterNames = Val(users().collect {
        case user if user.name.length == 5 => user.name()
      })
      fiveLetterNames() should be(List("Betty", "Chris", "Debby"))
    }
    "use syntactic sugar to add to a list" in {
      val v = Var[List[Int]](Nil)
      v += 5
      v.length should be(1)
      v.head should be(5)
    }
    "create a nested observing scenario" in {
      val screen1 = new Screen
      val screen2 = new Screen
      val screen3 = new Screen
      val screens: Var[List[Screen]] = Var(Nil)
      val active = Val(screens.filter(_.active()))
      screens := List(screen1, screen2, screen3)
      active() should be(Nil)
      screen2.active := true
      active() should be(List(screen2))
      screen1.active := true
      active() should be(List(screen1, screen2))
    }
    "test iterative observing scenario" in {
      val adjusts = Var(Vector(Var(0), Var(0), Var(0), Var(0), Var(0)))
      var previous = adjusts.head
      previous := 5
      adjusts.tail.foreach { v =>
        v := previous + 5
        previous = v
      }

      adjusts(0)() should be(5)
      adjusts(1)() should be(10)
      adjusts(2)() should be(15)
      adjusts(3)() should be(20)
      adjusts(4)() should be(25)
    }
    "test using 'once'" in {
      val v = Var(0)
      var fired = 0
      v.once { value =>
        fired = value
      }
      v := 1
      fired should be(1)
      v := 2
      fired should be(1)
    }
    "test using 'future'" in {
      val v = Var(0)
      val f = v.future()
      f.isCompleted should be(false)
      v := 1
      f.isCompleted should be(true)
      val result = f.value.get.get
      result should be(1)
      v := 2
      f.value.get.get should be(1)
    }
    "test stopping propagation" in {
      val v = Var(0)
      var invoked = false
      val first = v.on {
        v.stopPropagation()
      }
      val second = v.on {
        invoked = true
      }
      v.reactions() should be(List(first, second))
      v := 1
      invoked should be(false)
    }
    "test prioritization" in {
      val v = Var(0)
      val order = ListBuffer.empty[String]
      v.on({
        order += "highest"
      }, Priority.Highest)
      v.on({
        order += "normal"
      }, Priority.Normal)
      v.on({
        order += "low"
      }, Priority.Low)
      v.on({
        order += "high"
      }, Priority.High)
      v.on({
        order += "lowest"
      }, Priority.Lowest)
      v := 1

      order.toList should be(List("lowest", "low", "normal", "high", "highest"))
    }
    "test simple wrapping" in {
      val v1 = Var(1)
      val v2 = Var(2)
      val v3 = Var(3)

      val modified = ListBuffer.empty[Int]

      VarGroup(None, List(v1, v2, v3)).attach { i =>
        modified += i
      }

      v2 := 22
      v3 := 33
      v1 := 11
      modified.toList should be(List(22, 33, 11))
    }
    "test dsl wrapping" in {
      val v1 = Var(1)
      val v2 = Var(2)
      val v3 = Var(3)

      val modified = ListBuffer.empty[Int]

      v1.and(v2).and(v3).attach { i =>
        modified += i
      }

      v2 := 22
      v3 := 33
      v1 := 11
      modified.toList should be(List(22, 33, 11))
    }
    "validate complex hierarchical nesting" in {
      class Complex {
        val screen: Var[Option[Screen]] = Var[Option[Screen]](None)
      }
      val complex: Var[Option[Complex]] = Var[Option[Complex]](None)
      var active = false
      val enabled: Val[Boolean] = Val(complex.flatMap(_.screen().map(_.active())).getOrElse(false))
      enabled.attach(active = _)
      active should be(false)
      enabled() should be(false)
      val c = new Complex
      complex := Some(c)
      active should be(false)
      enabled() should be(false)
      val s = new Screen
      c.screen := Some(s)
      active should be(false)
      enabled() should be(false)
      s.active := true
      active should be(true)
      enabled() should be(true)
    }
    "verify dependency value properly updates dependant when using lazy" in {
      val v = Var[Double](lazyDouble)
      lazyDouble := 100.0
      v() should be(100.0)
    }
  }

  class Container[Child] {
    val children: Var[Vector[Child]] = Var[Vector[Child]](Vector.empty)
  }

  class User {
    val name: Var[String] = Var[String]("")
  }

  class Screen {
    val active: Var[Boolean] = Var(false)
  }
}