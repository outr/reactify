package reactify

class Reactions[T](reactive: Reactive[T]) {
  def apply(): List[Reaction[T]] = reactive._reactions

  def +=(reaction: Reaction[T]): Reaction[T] = synchronized {
    reactive._reactions = (reactive._reactions ::: List(reaction)).sorted.distinct
    reaction
  }

  def -=(reaction: Reaction[T]): Boolean = synchronized {
    val previous = reactive._reactions
    reactive._reactions = reactive._reactions.filterNot(_ eq reaction)
    previous != reactive._reactions
  }

  def clear(): Unit = synchronized {
    reactive._reactions = Nil
  }
}
