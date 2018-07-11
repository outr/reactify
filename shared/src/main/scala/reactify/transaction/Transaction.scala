package reactify.transaction

import reactify.Var

class Transaction {
  private var map = Map.empty[Var[_], TransactionChange]

  /**
    * Called when the value of a Var changes
    */
  def change[T](owner: Var[T], oldFunction: () => T, newFunction: () => T): Unit = {
    val change = map.get(owner) match {
      case Some(c) => c.copy(apply = () => owner := newFunction())
      case None => TransactionChange(() => owner := oldFunction(), () => owner := newFunction())
    }
    map += owner -> change
  }

  /**
    * Gets the `TransactionChange` for the supplied `Var` if one is defined
    */
  def get[T](v: Var[T]): Option[TransactionChange] = map.get(v)

  /**
    * Returns the `TransactionChange` for the supplied `Var` or throws an exception if none exists
    */
  def apply[T](v: Var[T]): TransactionChange = get(v).getOrElse(throw new RuntimeException(s"No reference in transaction for $v"))

  /**
    * Commits all changes in this Transaction and then clears the transaction
    */
  def commit(): Unit = {
    map.keys.foreach { v =>
      commit(v.asInstanceOf[Var[Any]])
    }
    map = Map.empty
  }

  /**
    * Reverts all changes in this Transaction and then clears the transaction
    */
  def revert(): Unit = {
    map.keys.foreach { v =>
      revert(v.asInstanceOf[Var[Any]])
    }
    map = Map.empty
  }

  /**
    * Undoes all changes that occurred within this Transaction. Unlike `revert`, this doesn't clear the transaction.
    * This allows `redo` to run to re-apply the transaction in the future.
    */
  def undo(): Unit = {
    map.keys.foreach { v =>
      undo(v.asInstanceOf[Var[Any]])
    }
  }

  /**
    * Redoes all changes that occurred within this Transaction. Unlike `commit`, this doesn't clear the transaction.
    * This allows `undo` to un-apply the transaction in the future.
    */
  def redo(): Unit = {
    map.keys.foreach { v =>
      redo(v.asInstanceOf[Var[Any]])
    }
  }

  /**
    * Redoes the transaction for this `Var` and then clears it from the transaction.
    *
    * @return true if a change was applied
    */
  def commit[T](v: Var[T]): Boolean = if (redo(v)) {
    map -= v
    true
  } else {
    false
  }

  /**
    * Undoes the transaction for this `Var` and then clears it from the transaction.
    *
    * @return true if a change was applied
    */
  def revert[T](v: Var[T]): Boolean = if (undo(v)) {
    map -= v
    true
  } else {
    false
  }

  /**
    * Undoes the transaction for this `Var`.
    */
  def undo[T](v: Var[T]): Boolean = get(v) match {
    case Some(change) => {
      change.unapply()
      true
    }
    case None => false
  }

  /**
    * Redoes the transaction for this `Var`.
    */
  def redo[T](v: Var[T]): Boolean = get(v) match {
    case Some(change) => {
      change.apply()
      true
    }
    case None => false
  }
}

/**
  * Transaction allows access to undo, redo, revert, and commit changes to `Var`s
  */
object Transaction {
  private val threadLocal = new ThreadLocal[Option[Transaction]] {
    override def initialValue(): Option[Transaction] = None
  }

  /**
    * True if a Transaction is currently active on the current thread
    */
  def active: Boolean = threadLocal.get().nonEmpty

  /**
    * Creates a new Transaction if one isn't already active or re-uses an existing one if a Transaction is already
    * in-progress for this thread.
    *
    * @param f the function to run within a Transaction
    * @return Transaction
    */
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

  /**
    * Called when the value of a Var changes
    */
  def change[T](owner: Var[T], oldFunction: () => T, newFunction: () => T): Unit = {
    threadLocal.get().foreach(_.change(owner, oldFunction, newFunction))
  }
}