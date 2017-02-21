package reactify

import java.util.concurrent.atomic.AtomicInteger

trait Observable[T] {
  val id: Int = Observable.nextId

  def attach(f: T => Unit): Listener = Observable.attach[T](id, f)

  def detach(listener: Listener): Unit = Observable.detach(listener)

  def changes(listener: ChangeListener[T]): Listener = attach(ChangeListener.createFunction(listener, None))

  protected def fire(value: T): Unit = Observable.fire(id, value)
}

object Observable {
  private var observers = Set.empty[Listener]
  private val increment = new AtomicInteger(0)

  private def nextId: Int = increment.incrementAndGet()

  private def fire[T](id: Int, value: T): Unit = observers.foreach(_.receive(id, value))

  def attach[T](id: Int, f: T => Unit): Listener = {
    val listener: Listener = (eventId: Int, value: Any) => if (eventId == id) {
      f(value.asInstanceOf[T])
    }
    attach(listener)
  }

  def attach(listener: Listener): Listener = synchronized {
    observers += listener
    listener
  }

  def detach(listener: Listener): Unit = synchronized {
    observers -= listener
  }
}

trait Listener {
  def receive(id: Int, value: Any): Unit
}