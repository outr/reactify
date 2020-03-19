package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.Var
import reactify.transaction.Transaction

class TransactionSpec extends AnyWordSpec with Matchers {
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
