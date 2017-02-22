package reactify

object Test {
  def main(args: Array[String]): Unit = {
    val v1 = Var(5)
    println(s"v1 observing ids: ${v1.observing}")
    v1.attach { value =>
      println(s"v1 changed to $value")
    }
    println(s"${v1.get} should be 5")
    v1.set(10)
    println(s"v1 observing ids: ${v1.observing}")
    println(s"${v1.get} should be 10")

    val v2 = Var(v1.get + 5)
    v2.attach { value =>
      println(s"v2 changed to $value")
    }
    println(s"${v2.get} should be 15")
    println(s"v2 observing ids: ${v2.observing}")
    v2.set(v1.get + 10)
    println(s"${v2.get} should be 20")

    v2.set(v2.get + v1.get)
    println(s"${v2.get} should be 30")

    v1.set(5)
    v2.set(10)

    val v3 = Var(v1.get + v2.get)
    println(s"${v3.get} should be 15")
    v3.set(v3() + 5)
    println(s"${v3.get} should be 20")
    v2.set(5)
    println(s"${v3.get} should be 15")
  }
}