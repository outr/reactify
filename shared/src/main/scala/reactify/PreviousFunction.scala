package reactify

class PreviousFunction[T](val function: () => T,
                          val previous: Option[PreviousFunction[T]])
