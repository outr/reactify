package reactify.reaction

/**
  * Reactions represents a list of Reaction instances specifically associated with a Reactive
  */
class Reactions[T] {
  // The reactions in firing order, in an array that grows as it fills, and an index of them by what they are equal by
  // (`keyOf`), so that adding one after the rest (most of them, since most reactions share a priority), finding whether
  // an equal one is here and taking one out cost the same however many there are. A value every screen depends on (a
  // theme's colour) carries a reaction for each thing bound to it, thousands, and a screen taken apart takes one out
  // for each of its own: a pass over the list for each of those kept a Roku past its watchdog. A reaction taken out is
  // marked dead in its entry, and the dead are swept out once they are as many as the living.
  private var slots: Array[ReactionEntry[T]] = new Array[ReactionEntry[T]](4)
  private var used: Int = 0
  private var dead: Int = 0
  // the living entries by their key's bucket
  private var buckets: Array[List[ReactionEntry[T]]] = Reactions.emptyBuckets[T](8)
  private var living: Int = 0
  // the list handed out, made again only after a change: a fire under way keeps the list it began with
  private var listed: List[Reaction[T]] = Nil
  private var stale: Boolean = false

  /**
    * Return all Reactions associated with this Reactive
    */
  def apply(): List[Reaction[T]] = synchronized {
    if (stale) {
      listed = slots.take(used).toList.filter(_.live).map(_.reaction)
      stale = false
    }
    listed
  }

  /**
    * Add a Reaction
    */
  def +=(reaction: Reaction[T]): Reaction[T] = synchronized {
    // The list is kept sorted by priority and distinct (by equality), so the reaction goes where a stable sort of the
    // list with it appended would put it (after every reaction that does not compare above it), unless an equal one is
    // already there. An equal one has the same key, so only those sharing its bucket are compared.
    val key = Reactions.keyOf(reaction)
    if (!bucket(key).exists(e => e.key == key && e.reaction == reaction)) place(reaction, key)
    reaction
  }

  /** Add a reaction known not to be here already (a Val's own, on a value it has just come to depend on). */
  private[reactify] def append(reaction: Reaction[T]): Unit = synchronized(place(reaction, Reactions.keyOf(reaction)))

  private def bucket(key: Int): List[ReactionEntry[T]] = buckets(Reactions.slot(key, buckets.length))

  private def place(reaction: Reaction[T], key: Int): Unit = {
    val entry = new ReactionEntry[T](reaction, key)
    if (used == slots.length) grow()
    val last = lastLive
    if (last < 0 || slots(last).reaction.compare(reaction) <= 0) {
      slots(used) = entry
      used += 1
    } else {
      // before the first living reaction that compares above it
      var at = 0
      while (at < used && (!slots(at).live || slots(at).reaction.compare(reaction) <= 0)) at += 1
      var i = used
      while (i > at) {
        slots(i) = slots(i - 1)
        i -= 1
      }
      slots(at) = entry
      used += 1
    }
    living += 1
    // (the index made again includes this entry, which is in its place already)
    if (living > buckets.length * 2) rehash(buckets.length * 4)
    else {
      val b = Reactions.slot(key, buckets.length)
      buckets(b) = entry :: buckets(b)
    }
    stale = true
  }

  // the last living entry's place, or -1
  private def lastLive: Int = {
    var i = used - 1
    while (i >= 0 && !slots(i).live) i -= 1
    i
  }

  // room for one more: the dead swept out, and the array doubled if that was not enough
  private def grow(): Unit = {
    if (dead > 0) sweep()
    if (used == slots.length) {
      val bigger = new Array[ReactionEntry[T]](slots.length * 2)
      var i = 0
      while (i < used) {
        bigger(i) = slots(i)
        i += 1
      }
      slots = bigger
    }
  }

  // the living moved up over the dead, in their order
  private def sweep(): Unit = {
    var to = 0
    var i = 0
    while (i < used) {
      val e = slots(i)
      if (e.live) {
        slots(to) = e
        to += 1
      }
      i += 1
    }
    while (used > to) {
      used -= 1
      slots(used) = null
    }
    dead = 0
  }

  // the index made again with `size` buckets
  private def rehash(size: Int): Unit = {
    val fresh = Reactions.emptyBuckets[T](size)
    var i = 0
    while (i < used) {
      val e = slots(i)
      if (e.live) {
        val b = Reactions.slot(e.key, size)
        fresh(b) = e :: fresh(b)
      }
      i += 1
    }
    buckets = fresh
  }

  /**
    * Remove a Reaction
    */
  def -=(reaction: Reaction[T]): Boolean = synchronized {
    val key = Reactions.keyOf(reaction)
    val b = Reactions.slot(key, buckets.length)
    val here = buckets(b)
    if (!here.exists(_.reaction eq reaction)) false
    else {
      val kept = here.filterNot(_.reaction eq reaction)
      here.foreach(e => if (e.reaction eq reaction) e.kill())
      val gone = here.size - kept.size
      buckets(b) = kept
      living -= gone
      dead += gone
      stale = true
      // (and the list handed out let go: a fire under way has its own, and the next is made afresh)
      listed = Nil
      if (dead > 8 && dead * 2 > used) sweep()
      true
    }
  }

  /**
    * Remove all Reactions
    */
  def clear(): Unit = synchronized {
    var i = 0
    while (i < used) {
      slots(i).kill()
      slots(i) = null
      i += 1
    }
    used = 0
    dead = 0
    living = 0
    buckets = Reactions.emptyBuckets[T](8)
    listed = Nil
    stale = false
  }
}

object Reactions {
  /** What a reaction is equal to others by, as a number: its function for the function reactions `attach` and `changes`
    * make (two of them are equal when their functions are), and the reaction itself for anything else. Equal reactions
    * have the same key, and most of those that are not have different ones. */
  private[reaction] def keyOf(reaction: Reaction[_]): Int = reaction match {
    case r: FunctionReaction[_] => System.identityHashCode(r.f)
    case r: ChangeFunctionReaction[_] => System.identityHashCode(r.f)
    case r => System.identityHashCode(r)
  }

  private[reaction] def slot(key: Int, size: Int): Int = {
    val m = key % size
    if (m < 0) m + size else m
  }

  private[reaction] def emptyBuckets[T](size: Int): Array[List[ReactionEntry[T]]] = {
    val b = new Array[List[ReactionEntry[T]]](size)
    var i = 0
    while (i < size) {
      b(i) = Nil
      i += 1
    }
    b
  }
}
