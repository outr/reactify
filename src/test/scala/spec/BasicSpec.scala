package specs

import reactify._
import org.scalatest.{Matchers, WordSpec}
import reactify.bind._
import reactify.instance.RecursionMode

import scala.collection.mutable.ListBuffer

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
    }
    "contain the proper value when added" in {
      val v1 = Val(5)
      val v2 = Val(v1 + 5)
      v2() should be(10)
    }
    "statically assign properly" in {
      val v1 = Var.apply(5)
      val v2 = Var.apply(5)
      val v3 = Val(v1 + v2, static = true)
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
    "statically assign properly" in {
      val v1 = Var(5)
      val v2 = Var(5)
      val v3 = Var(v1 + v2, static = true)
      v3.get should be(10)
      v1 := 10
      v3.get should be(10)
      v3.static(v1 + v2)
      v3.get should be(15)
      v2 := 10
      v3.get should be(15)
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
      val list = Var(List.empty[String], recursion = RecursionMode.Full)
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
      container.children.observing should be(Set(v1, v2))
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
      complex.observing should be(Set(v1, v3))  // Happens at runtime, so in its current state it only watches v1 and v3
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

      val fiveLetterNames = Val(users.collect {
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
      active.observing should be(Set(screens))
      screens := List(screen1, screen2, screen3)
      active.observing should be(Set(screens, screen1.active, screen2.active, screen3.active))
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
    /*"test a dirty var" in {
      val v = Var.dirty[Int](0)
      var firedFor = ListBuffer.empty[Int]
      v.attach(i => firedFor += i)
      v := 1
      firedFor.toList should be(Nil)
      v.update()
      firedFor.toList should be(List(1))
      v := 2
      firedFor.toList should be(List(1))
      v := 3
      firedFor.toList should be(List(1))
      v := 4
      firedFor.toList should be(List(1))
      v.update()
      firedFor.toList should be(List(1, 4))
    }*/
    "test stopping propagation" in {
      val v = Var(0)
      var invoked = false
      v.on {
        Invocation().stopPropagation()
      }
      v.on {
        invoked = true
      }
      v := 1
      invoked should be(false)
    }
    "test prioritization" in {
      val v = Var(0)
      val order = ListBuffer.empty[String]
      v.on({
        order += "highest"
      }, Listener.Priority.Highest)
      v.on({
        order += "normal"
      }, Listener.Priority.Normal)
      v.on({
        order += "low"
      }, Listener.Priority.Low)
      v.on({
        order += "high"
      }, Listener.Priority.High)
      v.on({
        order += "lowest"
      }, Listener.Priority.Lowest)
      v := 1

      order.toList should be(List("lowest", "low", "normal", "high", "highest"))
    }
    "test simple wrapping" in {
      val v1 = Var(1)
      val v2 = Var(2)
      val v3 = Var(3)

      val modified = ListBuffer.empty[Int]

      Observable.wrap(v1, v2, v3).attach { i =>
        modified += i
      }

      v2 := 22
      v3 := 33
      v1 := 11
      modified.toList should be(List(22, 33, 11))
    }
    // TODO: add proper support for wrapping covariant
    /*"test different typed wrapping" in {
      val v1 = Var(1)
      val v2 = Var("two")

      val modified = ListBuffer.empty[Any]

      Observable.wrap(v1, v2).attach { v =>
        modified += v
      }

      v1 := 11
      v2 := "two two"
      modified.toList should be(List(11, "two two"))
    }*/
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
      val enabled: Val[Boolean] = Val(complex.flatMap(_.screen.map(_.active())).getOrElse(false))
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
  }
  /*"Triggers" should {
    "handle simple invocations" in {
      val t = Trigger()
      var invoked = 0
      t.attach(invoked += 1)
      t.fire()
      invoked should be(1)
      t.fire()
      t.fire()
      invoked should be(3)
    }
  }*/
  "Bindings" when {
    "dealing with a simple binding" should {
      val a = Var[String]("a")
      val b = Var[String]("b")
      var binding: Binding[String, String] = null
      "have the proper initial values" in {
        a() should be("a")
        b() should be("b")
      }
      "bind the two values" in {
        binding = a bind b
        a() should be("a")
        b() should be("a")
      }
      "propagate a -> b" in {
        a := "one"
        a() should be("one")
        b() should be("one")
      }
      "propagate b -> a" in {
        b := "two"
        a() should be("two")
        b() should be("two")
      }
      "detach the binding" in {
        binding.detach()
      }
      "verify a -> b no longer propagates" in {
        a := "three"
        a() should be("three")
        b() should be("two")
      }
      "verify b -> a no longer propagates" in {
        b := "four"
        a() should be("three")
        b() should be("four")
      }
    }
    "dealing with binding between different types" should {
      val a = Var[String]("5")
      val b = Var[Int](10)
      var binding: Binding[String, Int] = null

      implicit val s2i: String => Int = (s: String) => Integer.parseInt(s)
      implicit val i2s: Int => String = (i: Int) => i.toString

      "have the proper initial values" in {
        a() should be("5")
        b() should be(10)
      }
      "bind the two values" in {
        binding = a bind b
        a() should be("5")
        b() should be(5)
      }
      "propagate a -> b" in {
        a := "25"
        a() should be("25")
        b() should be(25)
      }
      "propagate b -> a" in {
        b := 50
        a() should be("50")
        b() should be(50)
      }
      "detach the binding" in {
        binding.detach()
      }
      "verify a -> b no longer propagates" in {
        a := "100"
        a() should be("100")
        b() should be(50)
      }
      "verify b -> a no longer propagates" in {
        b := "200"
        a() should be("100")
        b() should be(200)
      }
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