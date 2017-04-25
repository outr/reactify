package reactify

import scala.annotation.tailrec

trait Transformable[T] extends Channel[T] {
  private[reactify] var transformers = List.empty[TransformingListener[T]]

  object transform {
    def attach(f: TransformableValue[T] => TransformResult[T],
               priority: Double = Listener.Priority.Normal): TransformingListener[T] = {
      observe(TransformingListener[T](f, priority))
    }

    def observe(listener: TransformingListener[T]): TransformingListener[T] = synchronized {
      transformers = (transformers ::: List(listener)).sorted
      listener
    }

    def detach(listener: TransformingListener[T]): Unit = synchronized {
      transformers = transformers.filterNot(_ eq listener)
    }

    def clear(): Unit = synchronized {
      transformers = List.empty
    }
  }

  override protected[reactify] def fire(value: T): Unit = {
    fireTransformRecursive(value, transformers) match {
      case Some(v) => super.fire(v)
      case None => // Cancelled
    }
  }

  @tailrec
  final protected def fireTransformRecursive(value: T, observers: List[TransformingListener[T]]): Option[T] = {
    if (observers.nonEmpty) {
      val listener = observers.head
      val updated: Option[T] = listener(TransformableValue(value)).value
      updated match {
        case None => None // Stop recursion
        case Some(v) => fireTransformRecursive(v, observers.tail)
      }
    } else {
      Some(value)
    }
  }
}

object Transformable {
  /**
    * Creates a new Transformable Channel.
    */
  def apply[T]: Transformable[T] = new Transformable[T] {
    override def set(value: => T): Unit = fire(value)
  }
}