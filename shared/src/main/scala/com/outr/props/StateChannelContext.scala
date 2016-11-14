package com.outr.props

/**
  * StateChannelContext is used internally by `MonitoringState` to track the `Observables` during assignment of a value
  * function.
  */
class StateChannelContext {
  var observables = Set.empty[Observable[_]]
}
