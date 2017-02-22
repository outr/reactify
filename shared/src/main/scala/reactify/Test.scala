package reactify

object Test {
  def main(args: Array[String]): Unit = {
    def equality[T](s: State[T], expect: T, name: String): Unit = {
      if (s() != expect) {
        println(s"*** $name expected $expect but got ${s()} ***")
      }
    }

    val v1 = Var(5)
    println(s"v1 observing ids: ${v1.observing}")
    v1.attach { value =>
      println(s"v1 changed to $value")
    }
    equality(v1, 5, "v1")
    v1.set(10)
    println(s"v1 observing ids: ${v1.observing}")
    equality(v1, 10, "v1")

    val v2 = Var(v1.get + 5)
    v2.attach { value =>
      println(s"v2 changed to $value")
    }
    equality(v2, 15, "v2")
    println(s"v2 observing ids: ${v2.observing}")
    v2.set(v1.get + 10)
    equality(v2, 20, "v2")

    v2.set(v2.get + v1.get)
    equality(v2, 30, "v2")

    v1.set(5)
    v2.set(10)

    val v3 = Var(v1.get + v2.get)
    v3.attach { value =>
      println(s"v3 changed to $value")
    }
    equality(v3, 15, "v3")
    v2.set(20)
    equality(v3, 25, "v3")
    v3.set(v3() + 5)
    equality(v3, 30, "v3")
    v2.set(5)
    equality(v3, 15, "v3")
  }
}