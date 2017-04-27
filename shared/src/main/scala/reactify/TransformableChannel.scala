package reactify

import scala.annotation.tailrec

/**
  * TransformableChannel extends from Channel to provide transforming capabilities before listeners are invoked.
  *
  * @tparam T the channel's type
  */
trait TransformableChannel[T] extends Channel[T] {
  private[reactify] var transformers = List.empty[TransformingListener[T]]

  /**
    * Functionality for transformations
    */
  object transform {
    /**
      * Attaches a simple functional transformation. Wraps `observe` as a convenience method.
      *
      * @param f function that gets converted to a TransformingListener
      * @param priority the priority. Defaults to Normal.
      * @return the created TransformingListener
      */
    def attach(f: TransformableValue[T] => TransformResult[T],
               priority: Double = Listener.Priority.Normal): TransformingListener[T] = {
      observe(TransformingListener[T](f, priority))
    }

    /**
      * Adds a TransformingListener to this channel that will be invoked before any listeners are called.
      *
      * @param listener the listener to add
      * @return the listener supplied
      */
    def observe(listener: TransformingListener[T]): TransformingListener[T] = synchronized {
      transformers = (transformers ::: List(listener)).sorted
      listener
    }

    /**
      * Detaches a TransformingListener from this TransformableChannel.
      *
      * @param listener the listener to detach
      */
    def detach(listener: TransformingListener[T]): Unit = synchronized {
      transformers = transformers.filterNot(_ eq listener)
    }

    /**
      * Clears all added transforming listeners from this TransfomableChannel.
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
        o.foreach(TransformableChannel.super.fire)
      }
      o
    }
  }

  override protected[reactify] def fire(value: T): Unit = transform(value)

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

object TransformableChannel {
  /**
    * Creates a new Transformable Channel.
    */
  def apply[T]: TransformableChannel[T] = new TransformableChannel[T] {
    override def set(value: => T): Unit = fire(value)
  }
}