package reactify

import scala.concurrent.{ExecutionContext, Future}

trait Mutable[T] {
  def set(f: => T): Unit
  def static(f: T): Unit = set(f)

  def :=(f: => T): Unit = set(f)
  def @=(f: T): Unit = static(f)

  /**
    * Convenience functionality to assign the result of a future (upon completion) to this Channel
    */
  def !(future: Future[T])(implicit ec: ExecutionContext): Future[Unit] = future.map { value =>
    set(value)
  }
}