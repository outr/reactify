package reactify

class Invocation private() {
  private var stopped = false

  def isStopped: Boolean = stopped
  def stopPropagation(): Unit = stopped = true

  private[reactify] def reset(): Invocation = {
    stopped = false
    this
  }
}

object Invocation {
  private val instances = new ThreadLocal[Invocation] {
    override def initialValue(): Invocation = new Invocation
  }

  def apply(): Invocation = instances.get
}