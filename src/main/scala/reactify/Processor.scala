package reactify

trait Processor[Input, Output, Processed] {
  private[reactify] var reactors = List.empty[Reactor[Input, Output]]

  protected def process(input: Input): Processed

  def attach(reactor: Reactor[Input, Output]): Reactor[Input, Output] = synchronized {
    reactors = (reactors ::: List(reactor)).sorted
    reactor
  }

  def detach(reactor: Reactor[Input, Output]): Unit = synchronized {
    reactors = reactors.filterNot(_ eq reactor)
  }

  def clear(): Unit = synchronized {
    reactors = List.empty
  }
}

trait Reactor[Input, Output] extends Ordered[Reactor[Input, Output]] {
  def apply(input: Input): Output

  def priority: Priority = Priority.Normal

  override def compare(that: Reactor[Input, Output]): Int = priority.value.compare(that.priority.value)
}

case class Priority(value: Double)

object Priority {
  lazy val Lowest: Priority = Priority(Double.MinValue)
  lazy val Low: Priority = Priority(-100.0)
  lazy val Normal: Priority = Priority(0.0)
  lazy val High: Priority = Priority(100.0)
  lazy val Highest: Priority = Priority(Double.MaxValue)
}