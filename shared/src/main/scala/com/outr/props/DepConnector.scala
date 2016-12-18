package com.outr.props

trait DepConnector[T, V] {
  def combine(variable: => V, adjustment: => T): T
  def extract(value: => T, adjustment: => T): V
}
