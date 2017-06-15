package reactify.instance

sealed trait StateInstance[T] {
  def isEmpty: Boolean = false
  def isUninitialized: Boolean = false
  protected def value: T
  def previous: StateInstance[T]

  def get: T = {
    StateInstanceManager.referenced(this)
    value
  }

  def reset(): Unit = {
    previous.reset()
  }

  def withPrevious(previous: StateInstance[T]): StateInstance[T]
}

class FunctionalInstance[T](f: () => T, val previous: StateInstance[T]) extends StateInstance[T] {
  override protected def value: T = f()

  def withPrevious(previous: StateInstance[T]): StateInstance[T] = new FunctionalInstance[T](f, previous)

  override def toString: String = s"FunctionalInstance(f: ${f()}, previous: $previous)"
}

class CachedInstance[T](override protected val value: T) extends StateInstance[T] {
  override def previous: StateInstance[T] = StateInstance.empty[T]

  def withPrevious(previous: StateInstance[T]): StateInstance[T] = new CachedInstance[T](value)

  override def toString: String = s"CachedInstance(value: $value)"
}

class UpdatableCachedFunctionalInstance[T](f: () => T,
                                           var cache: Option[T],
                                           val previous: StateInstance[T]) extends StateInstance[T] {
  override protected def value: T = cache match {
    case Some(v) => v
    case None => {
      val v = f()
      cache = Some(v)
      v
    }
  }

  def withPrevious(previous: StateInstance[T]): StateInstance[T] = {
    new UpdatableCachedFunctionalInstance[T](f, cache, previous)
  }

  override def reset(): Unit = {
    cache = None
    super.reset()
  }

  override def toString: String = s"UpdatableCachedFunctionalInstance(f: ${f()}, cache: $cache, previous: $previous)"
}

object EmptyStateInstance extends StateInstance[Any] {
  override def isEmpty: Boolean = true
  override protected def value: Any = throw new RuntimeException("EmptyStateInstance has no value!")
  def previous: StateInstance[Any] = throw new RuntimeException("EmptyStateInstance has no previous!")

  def withPrevious(previous: StateInstance[Any]): StateInstance[Any] = this

  override def reset(): Unit = {}

  override def toString: String = s"EmptyStateInstance"
}

object UninitializedStateInstance extends StateInstance[Any] {
  override def isUninitialized: Boolean = true
  override protected def value: Any = throw new RuntimeException("UninitializedStateInstance has no value!")
  def previous: StateInstance[Any] = throw new RuntimeException("UninitializedStateInstance has no previous!")

  def withPrevious(previous: StateInstance[Any]): StateInstance[Any] = this

  override def reset(): Unit = {}

  override def toString: String = s"UninitializedStateInstance"
}

object StateInstance {
  def empty[T]: StateInstance[T] = EmptyStateInstance.asInstanceOf[StateInstance[T]]
  def uninitialized[T]: StateInstance[T] = UninitializedStateInstance.asInstanceOf[StateInstance[T]]
  def functional[T](f: () => T, previous: StateInstance[T]): StateInstance[T] = new FunctionalInstance[T](f, previous)
  def cached[T](value: T): StateInstance[T] = new CachedInstance[T](value)
  def updatable[T](f: () => T, cache: Option[T], previous: StateInstance[T]): StateInstance[T] = {
    new UpdatableCachedFunctionalInstance[T](f, cache, previous)
  }
}