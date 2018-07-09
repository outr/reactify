package reactify.reaction

trait Reactions[T] {
  def apply(): List[Reaction[T]]
  def +=(reaction: Reaction[T]): Reaction[T]
  def -=(reaction: Reaction[T]): Boolean
  def clear(): Unit
}