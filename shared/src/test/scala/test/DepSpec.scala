package test

import org.scalatest.{Matchers, WordSpec}
import reactify._

import scala.collection.mutable.ListBuffer

class DepSpec extends WordSpec with Matchers {
  "Deps" should {
    val width: Var[Double] = Var(0.0)

    val left: Var[Double] = Var(0.0)
    val center: Dep[Double, Double] = Dep(left)(_ + (width / 2.0), _ - (width / 2.0))
    val right: Dep[Double, Double] = Dep(left)(_ + width, _ - width)

    val leftChanges = ListBuffer.empty[(Double, Double)]
    val centerChanges = ListBuffer.empty[(Double, Double)]
    val rightChanges = ListBuffer.empty[(Double, Double)]

    left.changes {
      case (oldValue, newValue) => leftChanges += oldValue -> newValue
    }
    center.changes {
      case (oldValue, newValue) => centerChanges += oldValue -> newValue
    }
    right.changes {
      case (oldValue, newValue) => rightChanges += oldValue -> newValue
    }

    def resetChanges(): Unit = {
      leftChanges.clear()
      centerChanges.clear()
      rightChanges.clear()
    }

    def changes(): List[List[(Double, Double)]] = List(leftChanges, centerChanges, rightChanges).map(_.toList)

    "have the basic values" in {
      left() should be(0.0)
      width() should be(0.0)
      center() should be(0.0)
      right() should be(0.0)

      changes() should be(List(
        Nil,
        Nil,
        Nil
      ))
    }
    "set left and reflect properly in right" in {
      resetChanges()

      left := 50.0
      left() should be(50.0)
      width() should be(0.0)
      center() should be(50.0)
      right() should be(50.0)

      changes() should be(List(
        List(0.0 -> 50.0),
        List(0.0 -> 50.0),
        List(0.0 -> 50.0)
      ))
    }
    "set width and reflect properly in right" in {
      resetChanges()

      width := 25.0
      List(left(), center(), right(), width()) should be(List(50.0, 62.5, 75.0, 25.0))

      changes() should be(List(
        Nil,
        List(50.0 -> 62.5),
        List(50.0 -> 75.0)
      ))
    }
    "set right and reflect properly in left" in {
      resetChanges()

      right := 100.0
      left() should be(75.0)
      width() should be(25.0)
      center() should be(87.5)
      right() should be(100.0)

      changes() should be(List(
        List(50.0 -> 75.0),
        List(62.5 -> 87.5),
        List(75.0 -> 100.0)
      ))
    }
    "set width again and not change left" in {
      resetChanges()

      width := 50.0
      left() should be(50.0)
      width() should be(50.0)
      center() should be(75.0)
      right() should be(100.0)

      changes() should be(List(
        List(75.0 -> 50.0),
        List(87.5 -> 75.0),
        Nil
      ))
    }
    "set center and reflect properly in left and right" in {
      resetChanges()

      center := 200.0
      left() should be(175.0)
      width() should be(50.0)
      center() should be(200.0)
      right() should be(225.0)

      changes() should be(List(
        List(50.0 -> 175.0),
        List(75.0 -> 200.0),
        List(100.0 -> 225.0)
      ))
    }
    "set width another time and retain center" in {
      resetChanges()

      width := 100.0
      left() should be(150.0)
      width() should be(100.0)
      center() should be(200.0)
      right() should be(250.0)

      changes() should be(List(
        List(175.0 -> 150.0),
        Nil,
        List(225.0 -> 250.0)
      ))
    }
    "set left and verify center and right adjust" in {
      resetChanges()

      left := 100.0
      left() should be(100.0)
      width() should be(100.0)
      center() should be(150.0)
      right() should be(200.0)

      changes() should be(List(
        List(150.0 -> 100.0),
        List(200.0 -> 150.0),
        List(250.0 -> 200.0)
      ))
    }
    "set width and retain left's position" in {
      resetChanges()

      width := 500.0
      left() should be(100.0)
      width() should be(500.0)
      center() should be(350.0)
      right() should be(600.0)

      changes() should be(List(
        Nil,
        List(150.0 -> 350.0),
        List(200.0 -> 600.0)
      ))
    }
  }
}
