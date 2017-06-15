package spec

import reactify._
import reactify.bind.Binding

object Test {
  def main(args: Array[String]): Unit = {
    val a = Var[String]("a")
    val b = Var[String]("b")
    var binding: Binding[String, String] = a bind b
    try {
      println("**** Assigning.... *********")
      a := "one"
      println(s"A: ${a()}, B: ${b()}")
    } catch {
      case t: StackOverflowError => println(s"Overflow!")
    }
  }
}
