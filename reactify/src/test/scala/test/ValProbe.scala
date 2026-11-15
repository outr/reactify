package test

/** A Val whose own `static` (not a Var's) a spec can call, and whose previous value and function it can read. */
class ValProbe[T](first: T) extends reactify.Val[T](first) {
  def setStatic(value: T): Unit = static(value)
  def previousNow: Option[T] = previous
  def functionNow: () => T = function
}
