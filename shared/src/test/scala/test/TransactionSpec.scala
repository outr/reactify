package test

import org.scalatest.{Matchers, WordSpec}
import reactify.Var
import reactify.transaction.Transaction

class TransactionSpec extends WordSpec with Matchers {
  "Transactions" should {
    "support undoing" in {
      val v = Var("One")
      val t = Transaction {
        v := "Two"
      }
      v() should be("Two")
      t.undo()
      v() should be("One")
    }
    "support redoing" in {
      val v = Var("One")
      val t = Transaction {
        v := "Two"
      }
      v() should be("Two")
      t.undo()
      v() should be("One")
      t.redo()
      v() should be("Two")
    }
  }
}
