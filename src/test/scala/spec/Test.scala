package spec

import reactify._

object Test {
  def main(args: Array[String]): Unit = {
    val v1 = Var(1)
    v1 := v1 * 2
    v1 := v1 * 3
    v1 := v1 * 4
    v1 := v1 * 5
    println(s"Value: ${v1()}")
  }
}
