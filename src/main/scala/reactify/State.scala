package reactify

case class State[T](function: () => T) {
  private var _value: T = _
  private var _references: List[Reactive[_]] = Nil

  def value: T = _value     // TODO: add to reference counter
  def references: List[Reactive[_]] = _references

  def update(): Unit = synchronized {
    // TODO: update the value and references
  }
}