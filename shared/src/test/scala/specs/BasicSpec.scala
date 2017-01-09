package specs

import com.outr.reactify._
import org.scalatest.{Matchers, WordSpec}

class BasicSpec extends WordSpec with Matchers {
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
  }
}
