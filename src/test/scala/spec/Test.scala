package spec

import reactify._

object Test {
  def main(args: Array[String]): Unit = {
    val v1 = Var("One")
//    v1.attach(s => println(s"NEW VALUE: $s"))
//    v1 := "Two"
    val v2 = Var("Two")
    val v3 = Val(v1() :: v2() :: Nil)
    println(v3())
    v2 := "Three"
    println(v3())
    v1 := "Two"
    println(v3())
  }
}
