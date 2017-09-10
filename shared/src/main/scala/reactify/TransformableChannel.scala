package reactify

import scala.annotation.tailrec

/**
  * TransformableChannel extends from Channel to provide transforming capabilities before observers are invoked.
  *
  * @tparam T the channel's type
  */
trait TransformableChannel[T] extends Channel[T] {
  private[reactify] var transformers = List.empty[TransformingObserver[T]]

  /**
    * Functionality for transformations
    */
  object transform {
    /**
      * Attaches a simple functional transformation. Wraps `observe` as a convenience method.
      *
      * @param f function that gets converted to a TransformingObserver
      * @param priority the priority. Defaults to Normal.
      * @return the created TransformingObserver
      */
    def attach(f: TransformableValue[T] => TransformResult[T],
               priority: Double = Observer.Priority.Normal): TransformingObserver[T] = {
      observe(TransformingObserver[T](f, priority))
    }

    /**
      * Adds a TransformingObserver to this channel that will be invoked before any observers are called.
      *
      * @param observer the observer to add
      * @return the observer supplied
      */
    def observe(observer: TransformingObserver[T]): TransformingObserver[T] = synchronized {
      transformers = (transformers ::: List(observer)).sorted
      observer
    }

    /**
      * Detaches a TransformingObserver from this TransformableChannel.
      *
      * @param observer the observer to detach
      */
    def detach(observer: TransformingObserver[T]): Unit = synchronized {
      transformers = transformers.filterNot(_ eq observer)
    }

    /**
      * Clears all added transforming observers from this TransformableChannel.
      */
    def clear(): Unit = synchronized {
      transformers = List.empty
    }

    /**
      * Applies transformations to the supplied value. The default operation of this method works exactly like calling
      * `fire` except you also get the benefit of receiving the transformation result.
      *
      * @param value the value to transform
      * @param fireResult whether the resulting value should be fired on the channel. Defaults to true.
      * @return the result of the transformations
      */
    def apply(value: T, fireResult: Boolean = true): Option[T] = {
      val o = fireTransformRecursive(value, transformers)
      if (fireResult) {
        o.foreach(TransformableChannel.super.fire(_, InvocationType.Direct))
      }
      o
    }
  }

  override protected[reactify] def fire(value: T, `type`: InvocationType): Unit = transform(value)

  @tailrec
  final protected def fireTransformRecursive(value: T, observers: List[TransformingObserver[T]]): Option[T] = {
    observers.headOption match {
      case Some(observer) => {
        val updated: Option[T] = observer(TransformableValue(value)).value
        updated match {
          case None => None // Stop recursion
          case Some(v) => fireTransformRecursive(v, observers.tail)
        }
      }
      case None => Some(value)
    }
  }
}

object TransformableChannel {
  /**
    * Creates a new Transformable Channel.
    */
  def apply[T]: TransformableChannel[T] = new TransformableChannel[T] {
    override def set(value: => T): Unit = fire(value, InvocationType.Direct)
  }
}