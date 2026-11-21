package reactify

/** What a spec may read of the depth budget: how deep a chain of fires nests before a link is deferred, and how many
  * deferred links wait on this thread. */
object DeferralProbe {
  def maxDepth: Int = Reactive.maxDepth

  def waiting: Int = {
    val c = Reactive.propagations.get()
    c.front.size + c.back.size
  }
}
