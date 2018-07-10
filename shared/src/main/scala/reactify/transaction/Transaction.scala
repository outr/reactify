package reactify.transaction

import reactify.Var

class Transaction {
  private var map = Map.empty[Var[_], TransactionChange]

  def change[T](owner: Var[T], oldFunction: () => T, newFunction: () => T): Unit = {
    val change = map.get(owner) match {
      case Some(c) => c.copy(apply = () => owner := newFunction())
      case None => TransactionChange(() => owner := oldFunction(), () => owner := newFunction())
    }
    map += owner -> change
  }

  def get[T](v: Var[T]): Option[TransactionChange] = map.get(v)

  def apply[T](v: Var[T]): TransactionChange = get(v).getOrElse(throw new RuntimeException(s"No reference in transaction for $v"))

  def commit(): Unit = {
    map.keys.foreach { v =>
      commit(v.asInstanceOf[Var[Any]])
    }
    map = Map.empty
  }

  def revert(): Unit = {
    map.keys.foreach { v =>
      revert(v.asInstanceOf[Var[Any]])
    }
    map = Map.empty
  }

  def undo(): Unit = {
    map.keys.foreach { v =>
      undo(v.asInstanceOf[Var[Any]])
    }
  }

  def redo(): Unit = {
    map.keys.foreach { v =>
      redo(v.asInstanceOf[Var[Any]])
    }
  }

  def commit[T](v: Var[T]): Boolean = if (redo(v)) {
    map -= v
    true
  } else {
    false
  }

  def revert[T](v: Var[T]): Boolean = if (undo(v)) {
    map -= v
    true
  } else {
    false
  }

  def undo[T](v: Var[T]): Boolean = get(v) match {
    case Some(change) => {
      change.unapply()
      true
    }
    case None => false
  }

  def redo[T](v: Var[T]): Boolean = get(v) match {
    case Some(change) => {
      change.apply()
      true
    }
    case None => false
  }
}

object Transaction {
  private val threadLocal = new ThreadLocal[Option[Transaction]] {
    override def initialValue(): Option[Transaction] = None
  }

  def active: Boolean = threadLocal.get().nonEmpty

  def apply(f: => Unit): Transaction = {
    val created = !active
    val transaction = threadLocal.get().getOrElse {
      val t = new Transaction
      threadLocal.set(Some(t))
      t
    }
    f
    if (created) {
      threadLocal.remove()
    }
    transaction
  }

  def change[T](owner: Var[T], oldFunction: () => T, newFunction: () => T): Unit = {
    threadLocal.get().foreach(_.change(owner, oldFunction, newFunction))
  }
}