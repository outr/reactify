package test

import testy._
import reactify.Var
import reactify.transaction.Transaction
import scala.language.implicitConversions

class TransactionSpec extends Spec {
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
