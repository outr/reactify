package com.outr

import scala.language.implicitConversions

package object props {
  implicit def state2Value[T](p: StateChannel[T]): T = p.get
}