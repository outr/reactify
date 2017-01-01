package specs

import com.outr.reactify._

object Test {
  def main(args: Array[String]): Unit = {
    val a = Var(5)
    val b = Var(5)

    b := a + a + 5
    println(b())
//    b.test(a + a + 5)
  }
}
