package com.outr.props

trait Observable[T] {
  private var observers = Set.empty[T => Unit]

  def attach(f: T => Unit): Unit = synchronized {
    observers += f
  }

  def detach(f: T => Unit): Unit = synchronized {
    observers -= f
  }

  protected def fire(value: T): Unit = observers.foreach { obs =>
    obs(value)
  }
}

trait Channel[T] extends Observable[T] {
  def :=(value: => T): Unit = fire(value)
}

trait StateChannel[T] extends Observable[T] {
  protected def state: T

  def get: T = {
    StateChannel.contextFired(this)
    state
  }

  def value: T = get
}

object StateChannel {
  private val context = new ThreadLocal[Option[StateChannelContext]] {
    override def initialValue(): Option[StateChannelContext] = None
  }

  def contextualized[R](f: => R): R = {
    val oldValue = context.get()
    context.set(Some(new StateChannelContext))
    try {
      f
    } finally {
      context.set(oldValue)
    }
  }

  def contextFired[T](channel: StateChannel[T]): Unit = context.get().foreach { c =>
    channel match {
      case o: Observable[_] => c.observables += o
      case _ => // Not an observable
    }
  }

  def contextObservables(): Set[Observable[_]] = context.get().getOrElse(throw new RuntimeException("Not within a context!")).observables
}

class StateChannelContext {
  var observables = Set.empty[Observable[_]]
}

trait MonitoringStateChannel[T] extends StateChannel[T] {
  private var monitoring = Set.empty[Observable[_]]
  private val monitorListener = (value: Any) => fire(get)

  protected def monitor(f: => T): T = synchronized {
    monitoring.foreach { o =>
      o.detach(monitorListener)
    }

    StateChannel.contextualized {
      val value: T = f
      val observables = StateChannel.contextObservables()
      observables.foreach { o =>
        o.attach(monitorListener)
      }
      monitoring = observables
      value
    }
  }
}

class Val[T] private(value: () => T) extends MonitoringStateChannel[T] {
  monitor(value())

  override protected def state: T = value()
}

object Val {
  def apply[T](value: => T): Val[T] = new Val[T](() => value)
}

class Var[T] private() extends Channel[T] with MonitoringStateChannel[T] {
  private var _value: () => T = _

  override protected def state: T = _value()

  override def :=(value: => T): Unit = {
    val v = monitor(value)
    _value = () => value
    super.:=(v)
  }
}

object Var {
  def apply[T](value: => T): Var[T] = {
    val v = new Var[T]()
    v := value
    v
  }
}