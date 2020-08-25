package reactify

import scala.concurrent.{ExecutionContext, Future}

/**
  * Mutable represents a reactive element that has mutable state
  */
trait Mutable[T] {
  /**
    * Sets the function evaluation representing the new value for this mutable entity
    */
  def set(f: => T): Unit

  /**
    * Sets a static value representing the new value for this mutable entity
    */
  def static(f: T): Unit = set(f)

  /**
    * Convenience alternative to "set"
    */
  def :=(f: => T): Unit = set(f)

  /**
    * Convenience alternative to "static"
    */
  def @=(f: T): Unit = static(f)

  /**
    * Convenience alternative to "set"
    */
  def update(f: => T): Unit = set(f)

  /**
    * Convenience functionality to assign the result of a future (upon completion) to this Channel
    */
  def !(future: Future[T])(implicit ec: ExecutionContext): Future[Unit] = future.map { value =>
    set(value)
  }
}