package reactify.prototype

object Test {
  def main(args: Array[String]): Unit = {
    val v = Var(1)
    println(s"Value: ${v.get}")
    v.set(2)
    println(s"Value: ${v.get}")
    v.set(3 + v.get)

    val v2 = Var(v.get + 5)
    v := 6
  }
}

class Var[T] private() {
  private var function: () => T = _
  private var evaluated: T = _
  private var previous: Option[T] = None
  private var references: Set[Var[_]] = Set.empty

  def set(value: => T): Unit = {
    function = () => value
    Var.evaluate(this, updating = false)
  }

  def static(value: T): Unit = {
    function = () => value
    previous = Option(evaluated)
    evaluated = value
    references = Set.empty
  }

  def get: T = Var.get(this)

  def :=(value: => T): Unit = set(value)
  def @=(value: T): Unit = static(value)
  def apply(): T = get

  def equality(t1: T, t2: T): Boolean = t1 == t2

  override def toString: String = s"Var($evaluated)"
}

object Var {
  private val evaluating: ThreadLocal[Option[Evaluating]] = new ThreadLocal[Option[Evaluating]] {
    override def initialValue(): Option[Evaluating] = None
  }

  def apply[T](value: => T): Var[T] = {
    val v = new Var[T]
    v.set(value)
    v
  }

  def evaluate[T](v: Var[T], updating: Boolean): Unit = {
    assert(evaluating.get().isEmpty, s"Expected empty evaluating, but found: ${evaluating.get()}")
    val e = new Evaluating(v, updating)
    evaluating.set(Some(e))
    val evaluated = try {
      v.function()
    } finally {
      evaluating.set(None)
    }
    if (evaluated != v.evaluated) {
      v.previous = Option(v.evaluated)
      v.evaluated = evaluated
      // TODO: Fire
      println(s"Modified! Previous: ${v.previous}, Current: $evaluated, References: ${e.references}")
    }
  }

  def get[T](v: Var[T]): T = {
    evaluating.get() match {
      case Some(e) if e.v eq v => {
        if (e.updating) {
          v.previous.getOrElse(throw new RuntimeException("Attempting to get previous on None!"))
        } else {
          v.evaluated
        }
      }
      case Some(e) => {
        e.references += v

        v.evaluated
      }
      case None => v.evaluated
    }
  }
}

class Evaluating(val v: Var[_], val updating: Boolean, var references: Set[Var[_]] = Set.empty)