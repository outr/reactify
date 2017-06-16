package reactify

trait DirtyState[T] extends AbstractState[T] {
  def isDirty: Boolean = manager.isDirty
  def update(): Boolean = manager.update()
}