package reactify.transaction

/**
  * TransactionChange represents the transactional changes for a single `Var` in a `Transaction`
  *
  * @param unapply reverts the changes applied during the transaction
  * @param apply applies the changes applied during the transaction
  */
case class TransactionChange(unapply: () => Unit, apply: () => Unit)