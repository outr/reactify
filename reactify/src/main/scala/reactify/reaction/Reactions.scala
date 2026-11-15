package reactify.reaction

import reactify.Reactive

/**
  * Reactions represents a list of Reaction instances specifically associated with a Reactive
  */
class Reactions[T] {
  // Most values have a reaction or two, and are kept as a plain list, which costs nothing beyond it. A value that comes to
  // have many (a theme's colour, one for each thing bound to it) moves them into an index (`ReactionIndex`), where adding
  // one and taking one out cost the same however many there are, and back to a list once few are left.
  private var list: List[Reaction[T]] = Nil
  private var index: Option[ReactionIndex[T]] = None

  /**
    * Return all Reactions associated with this Reactive
    */
  def apply(): List[Reaction[T]] = synchronized {
    index match {
      case Some(i) => i()
      case None => list
    }
  }

  /**
    * Add a Reaction
    */
  def +=(reaction: Reaction[T]): Reaction[T] = synchronized {
    index match {
      case Some(i) => i += reaction
      case None =>
        // The list is kept sorted by priority and distinct, so the reaction goes where a stable sort of the list with it
        // appended would put it (after every reaction that does not compare above it), unless the same one is already
        // there (`Reactions.same`).
        var rest = list
        while (rest.nonEmpty && !Reactions.same(rest.head, reaction)) rest = rest.tail
        if (rest.isEmpty) place(reaction)
    }
    reaction
  }

  /** Add a reaction known not to be here already (a Val's own, on a value it has just come to depend on). */
  private[reactify] def append(reaction: Reaction[T]): Unit = synchronized {
    index match {
      case Some(i) => i.append(reaction)
      case None => place(reaction)
    }
  }

  private def place(reaction: Reaction[T]): Unit = {
    if (list.isEmpty || list.last.compare(reaction) <= 0) list = list :+ reaction
    else {
      var at = 0
      var placing = true
      list.foreach { r =>
        if (placing && r.compare(reaction) <= 0) at += 1
        else placing = false
      }
      list = list.take(at) ::: (reaction :: list.drop(at))
    }
    if (list.size > Reactions.indexFrom) {
      val made = new ReactionIndex[T]
      list.foreach(r => made.append(r))
      index = Some(made)
      list = Nil
    }
  }

  /**
    * Remove a Reaction
    */
  def -=(reaction: Reaction[T]): Boolean = synchronized {
    index match {
      case Some(i) =>
        val removed = i -= reaction
        if (i.size < Reactions.indexTo) {
          list = i()
          index = None
        }
        removed
      case None =>
        val before = list.length
        list = list.filterNot(_ eq reaction)
        list.length != before
    }
  }

  /**
    * Remove all Reactions
    */
  def clear(): Unit = synchronized {
    index.foreach(_.clear())
    index = None
    list = Nil
  }

  /** How many reactions are left, as far as 2: what a value holding these as its own (`Reactive.heldReactions`) asks
    * as one is taken out, to hold the one left as it is, or none. (An index is never left with fewer than 8.) */
  private[reaction] def fewLeft: Int = synchronized {
    if (index.nonEmpty) 2 else if (list.isEmpty) 0 else if (list.tail.isEmpty) 1 else 2
  }
}

object Reactions {
  /** A value's own reactions (`Reactive.reactions`, where it is not overridden), kept on the value itself
    * (`Reactive.heldReactions`) rather than here: what a value with one reaction or none keeps is the reaction or
    * nothing, where it was a `Reactions` and a list cell for each, and this, made where something asks for them, is let
    * go of as soon as it has been used. A value with more holds a `Reactions` of them, which this hands each change on to;
    * one taken back to one or none holds that again. */
  final class OwnReactions[T](val owner: Reactive[T]) extends Reactions[T] {
    override def apply(): List[Reaction[T]] = owner.heldReactions match {
      case one: Reaction[T @unchecked] => one :: Nil
      case many: Reactions[T @unchecked] => many()
      case _ => Nil
    }

    override def +=(reaction: Reaction[T]): Reaction[T] = {
      hold(owner, reaction, known = false)
      reaction
    }

    override private[reactify] def append(reaction: Reaction[T]): Unit = hold(owner, reaction, known = true)

    override def -=(reaction: Reaction[T]): Boolean = release(owner, reaction)

    override def clear(): Unit = Reactions.synchronized {
      owner.heldReactions match {
        case many: Reactions[_] => many.clear()
        case _ => // nothing else to let go of
      }
      owner.heldReactions = Reactive.Unheard
    }
  }

  /** `reaction` added to `owner`'s reactions (`reactions += reaction`), without a `Reactions` made for it where `owner`
    * holds its own. */
  private[reactify] def add[T](owner: Reactive[T], reaction: Reaction[T]): Reaction[T] = {
    if (owner.heldReactions == null) owner.reactions += reaction
    else {
      hold(owner, reaction, known = false)
      reaction
    }
  }

  /** `reaction`, known not to be there already, added to `owner`'s reactions (`reactions.append`). */
  private[reactify] def append[T](owner: Reactive[T], reaction: Reaction[T]): Unit = {
    if (owner.heldReactions == null) owner.reactions.append(reaction)
    else hold(owner, reaction, known = true)
  }

  /** `reaction` taken out of `owner`'s reactions (`reactions -= reaction`). */
  private[reactify] def remove[T](owner: Reactive[T], reaction: Reaction[T]): Boolean = {
    if (owner.heldReactions == null) owner.reactions -= reaction
    else release(owner, reaction)
  }

  /** `owner`, found to keep its own reactions and to have none, known to have none. */
  private[reactify] def unheard(owner: Reactive[_]): Unit = synchronized {
    if (owner.heldReactions == null) owner.heldReactions = Reactive.Unheard
  }

  // Adding and taking out are as a `Reactions` of the value's own did: the same reaction (`same`) is not added twice
  // (unless it is known not to be there), the reactions are kept in priority order, first come first within one, and a
  // reaction is taken out by identity. The value's hold changes under this one lock; a fire reads it as it is.
  private def hold[T](owner: Reactive[T], reaction: Reaction[T], known: Boolean): Unit = synchronized {
    owner.heldReactions match {
      case one: Reaction[T @unchecked] =>
        if (known || !same(one, reaction)) {
          val many = new Reactions[T]
          many.append(one)
          if (known) many.append(reaction) else many += reaction
          owner.heldReactions = many
        }
      case many: Reactions[T @unchecked] => if (known) many.append(reaction) else many += reaction
      case _ => owner.heldReactions = reaction
    }
  }

  private def release[T](owner: Reactive[T], reaction: Reaction[T]): Boolean = synchronized {
    owner.heldReactions match {
      case one: Reaction[_] =>
        if (one eq reaction) {
          owner.heldReactions = Reactive.Unheard
          true
        } else {
          false
        }
      case many: Reactions[T @unchecked] =>
        val removed = many -= reaction
        if (removed) {
          val left = many.fewLeft
          if (left == 0) owner.heldReactions = Reactive.Unheard
          else if (left == 1) owner.heldReactions = many().head
        }
        removed
      case _ => false
    }
  }

  /** How many reactions a value keeps as a plain list before it indexes them, and how few it goes back to one at. */
  private[reaction] val indexFrom: Int = 32
  private[reaction] val indexTo: Int = 8

  /** What a reaction is equal to others by, as a number: its function for the function reactions `attach` and `changes`
    * make (two of them are equal when their functions are), and the reaction itself for anything else. Equal reactions
    * have the same key, and most of those that are not have different ones. */
  private[reaction] def keyOf(reaction: Reaction[_]): Int = reaction match {
    case r: FunctionReaction[_] => System.identityHashCode(r.f)
    case r: ChangeFunctionReaction[_] => System.identityHashCode(r.f)
    case r => System.identityHashCode(r)
  }

  /** Whether two reactions are the same, as a list holds each once: the very same reaction, or two function reactions
    * (what `attach` and `changes` make) of the same kind, the same function and the same priority, which is what their
    * equality came to (a function is equal only to itself). Asked of each reaction already there whenever one is added,
    * so it compares by identity and fields rather than by `==`, a case class's whole equality, which on a platform that
    * emulates it (a Roku) was most of what adding a reaction cost. Any other reaction is the same only as itself, as the
    * index (`keyOf`) already took it. */
  private[reaction] def same(a: Reaction[_], b: Reaction[_]): Boolean = if (a eq b) true else a match {
    case x: FunctionReaction[_] => b match {
      case y: FunctionReaction[_] => (x.f eq y.f) && x.priority == y.priority
      case _ => false
    }
    case x: ChangeFunctionReaction[_] => b match {
      case y: ChangeFunctionReaction[_] => (x.f eq y.f) && x.priority == y.priority
      case _ => false
    }
    case _ => false
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
