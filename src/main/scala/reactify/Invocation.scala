package reactify

class Invocation private() {
  private var stopped = false

  def isStopped: Boolean = stopped
  def stopPropagation(): Unit = stopped = true

  private[reactify] def wrap[R](f: => R): R = {
    val previous = stopped
    stopped = false
    try {
      f
    } finally {
      stopped = previous
    }
  }
}

object Invocation {
  private val instances = new ThreadLocal[Invocation] {
    override def initialValue(): Invocation = new Invocation
  }

  def apply(): Invocation = instances.get
}