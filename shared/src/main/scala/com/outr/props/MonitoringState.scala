package com.outr.props

/**
  * MonitoringState is a convenience wrapper around `State` providing functionality to monitor `Observables` relating
  * to the value from a function.
  *
  * @tparam T the type of value retained by this `State`
  */
trait MonitoringState[T] extends State[T] {
  private var monitoring = Set.empty[Observable[_]]
  private val monitorListener = (value: Any) => fire(get)

  /**
    * Called when assigning the value. This method removes any existing monitored `Observables`, determines the new
    * `Observables` from the supplied function `f`, and monitors changes to any of those.
    *
    * @param f the function to monitor
    * @return T
    */
  protected def monitor(f: => T): T = synchronized {
    monitoring.foreach { o =>
      o.detach(monitorListener)
    }

    State.contextualized {
      val value: T = f
      val observables = State.contextObservables()
      observables.foreach { o =>
        o.attach(monitorListener)
      }
      monitoring = observables
      value
    }
  }
}
