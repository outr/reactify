package reactify

import reactify.instance.StateInstanceManager

object Transaction {
  private val threadLocal = new ThreadLocal[Option[Transaction]] {
    override def initialValue(): Option[Transaction] = None
  }

  def apply[R](f: => R): R = if (!inTransaction) {
    val t = new Transaction()
    threadLocal.set(Some(t))
    try {
      val result: R = f
      t.commit()
      result
    } finally {
      threadLocal.remove()
    }
  } else {
    f
  }

  def withTransaction(f: => Unit, transaction: Transaction = new Transaction()): Transaction = {
    val previous = threadLocal.get()
    threadLocal.set(Some(transaction))
    try {
      f
    } finally {
      threadLocal.set(previous)
    }
    transaction
  }

  def inTransaction: Boolean = threadLocal.get().nonEmpty

  def update[T](manager: StateInstanceManager[T], f: Option[() => T]): Unit = threadLocal.get().get.update(manager, f)
}

class Transaction(private var map: Map[StateInstanceManager[_], TransactionApplier[_]] = Map.empty) {
  def update[T](manager: StateInstanceManager[T], f: Option[() => T]): Unit = {
    val function: Option[() => T] = f.orElse(map.get(manager).flatMap(_.f.asInstanceOf[Option[() => T]]))
    map += manager -> new TransactionApplier[T](manager, function)
  }

  def commit(): Unit = map.values.foreach(_.commit())
}

class TransactionApplier[T](manager: StateInstanceManager[T], val f: Option[() => T] = None) {
  def commit(): Unit = f match {
    case Some(function) => manager.replaceInstance(function, force = true)
    case None => manager.updateInstance(force = true)
  }
}