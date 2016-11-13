package com.outr.props

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
