package reactify.transaction

case class TransactionChange(unapply: () => Unit, apply: () => Unit)