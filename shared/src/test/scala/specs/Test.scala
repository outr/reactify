package specs

import reactify.{Dep, Var}

object Test {
  def main(args: Array[String]): Unit = {
    val left = Var(0.0)
    val width = Var(0.0)
    val center = Dep[Double, Double](left, width / 2.0)
    val right = Dep[Double, Double](left, width, submissive = true)

    def verify(label: String, l: Double, c: Double, r: Double, w: Double): Unit = {
      val list = List(l, c, r, w)
      val actual = List(left(), center(), right(), width())
      val equality = actual == list
      if (!equality) {
        println(s"$label - expected: $list, but received: $actual")
      }
    }

    println("*** Setting Center!")
    center := 500.0
    verify("Modified Center", 500.0, 500.0, 500.0, 0.0)

    println("*** Setting Width!")
    width := 100.0
    verify("Modified Width", 450.0, 500.0, 550.0, 100.0)

    println("*** Setting Right!")
    right := 600.0
    verify("Modified Right", 500.0, 550.0, 600.0, 100.0)

    println("*** Setting Width!")
    width := 200.0
    verify("Modified Width", 450.0, 550.0, 750.0, 200.0)
  }
}
